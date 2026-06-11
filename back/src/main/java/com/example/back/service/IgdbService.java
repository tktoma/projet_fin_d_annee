package com.example.back.service;

import com.example.back.dto.IgdbGameDto;
import com.example.back.dto.ImportProgress;
import com.example.back.dto.ImportResult;
import com.example.back.dto.JeuManuelRequest;
import com.example.back.dto.JeuResponse;
import com.example.back.dto.ResponseMapper;
import com.example.back.entities.Jeu;
import com.example.back.exception.NotFoundException;
import com.example.back.repository.JeuRepository;
import com.example.back.repository.JeuSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IgdbService {

    private static final Logger log = LoggerFactory.getLogger(IgdbService.class);

    private final WebClient igdbWebClient;
    private final JeuRepository jeuRepository;
    private final ImportProgress progress;
    private final ImportAutoService importAutoService;

    public IgdbService(WebClient igdbWebClient,
                       JeuRepository jeuRepository,
                       ImportProgress progress,
                       ImportAutoService importAutoService) {
        this.igdbWebClient = igdbWebClient;
        this.jeuRepository = jeuRepository;
        this.progress = progress;
        this.importAutoService = importAutoService;
    }

    // -------------------------------------------------------------------------
    // Recherche IGDB
    // -------------------------------------------------------------------------

    @Cacheable(value = "recherches-igdb", key = "#titre.toLowerCase().trim()",
            unless = "#result == null || #result.isEmpty()")
    public List<IgdbGameDto> rechercherJeu(String titre) {
        String body = "fields name,summary,cover.url,genres.name,platforms.name,first_release_date;"
                + " search \"" + titre + "\"; limit 10;";
        return igdbWebClient.post().uri("/games").bodyValue(body)
                .retrieve().bodyToFlux(IgdbGameDto.class)
                .collectList().defaultIfEmpty(List.of()).block();
    }

    @CacheEvict(value = "recherches-igdb", allEntries = true)
    public void viderCacheRecherches() {}

    // -------------------------------------------------------------------------
    // Lecture catalogue avec filtres + tri
    // -------------------------------------------------------------------------

    /**
     * @param sort "titre" | "noteMoyenne" | "dateSortie" | "vues" | "popularite"
     *             "popularite" = tri par nombre de personnes ayant le jeu en bibliothèque
     *             (nécessite un tri en mémoire car pas de colonne directe)
     */
    public Page<JeuResponse> rechercherAvecFiltres(
            String titre, String genre, String plateforme,
            Float noteMin, Integer anneeMin, Integer anneeMax,
            String sort, Pageable pageable) {

        Specification<Jeu> spec = Specification
                .where(JeuSpecification.titreLike(titre))
                .and(JeuSpecification.genreEgal(genre))
                .and(JeuSpecification.plateformeEgale(plateforme))
                .and(JeuSpecification.noteMoyenneMin(noteMin))
                .and(JeuSpecification.anneeMin(anneeMin))
                .and(JeuSpecification.anneeMax(anneeMax));

        // "popularite" = tri par nbBibliotheque — on charge tout puis on trie/pagine en mémoire
        if ("popularite".equals(sort)) {
            List<Jeu> all = jeuRepository.findAll(spec);
            List<JeuResponse> dtos = all.stream()
                    .map(j -> enrichWithStats(ResponseMapper.toJeuResponse(j), j.getId()))
                    .sorted((a, b) -> Long.compare(b.getNbBibliotheque(), a.getNbBibliotheque()))
                    .toList();
            int start = (int) pageable.getOffset();
            int end   = Math.min(start + pageable.getPageSize(), dtos.size());
            List<JeuResponse> page = start >= dtos.size() ? List.of() : dtos.subList(start, end);
            return new PageImpl<>(page, pageable, dtos.size());
        }

        // Tri standard
        Pageable sortedPageable = pageable;
        if ("vues".equals(sort)) {
            sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "vues"));
        } else if ("noteMoyenne".equals(sort)) {
            sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "noteMoyenne"));
        } else if ("dateSortie".equals(sort)) {
            sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "dateSortie"));
        }

        Page<Jeu> page = jeuRepository.findAll(spec, sortedPageable);
        List<JeuResponse> dtos = page.getContent().stream()
                .map(ResponseMapper::toJeuResponse).toList();
        return new PageImpl<>(dtos, sortedPageable, page.getTotalElements());
    }

    public List<String> getGenres() {
        return jeuRepository.findDistinctGenres();
    }

    public List<String> getPlateformes() {
        return jeuRepository.findDistinctPlateformes();
    }

    // -------------------------------------------------------------------------
    // Fiche détail — incrémente les vues + stats bibliothèque
    // -------------------------------------------------------------------------

    @Transactional
    public JeuResponse getJeuById(Long id) {
        Jeu jeu = jeuRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Jeu introuvable"));

        // Incrémente les vues à chaque consultation de la fiche
        jeuRepository.incrementVues(id);

        JeuResponse dto = ResponseMapper.toJeuResponse(jeu);
        dto.setVues(jeu.getVues() + 1); // refléter l'incrément
        return enrichWithStats(dto, id);
    }

    // -------------------------------------------------------------------------
    // Import unitaire
    // -------------------------------------------------------------------------

    public JeuResponse importerJeu(Long igdbId) {
        return ResponseMapper.toJeuResponse(importerEntite(igdbId));
    }

    public ImportResult importerJeuxPagines(int page, int limit) {
        int offset = page * limit;
        String body = "fields name,summary,cover.url,genres.name,platforms.name,first_release_date;"
                + " limit " + limit + "; offset " + offset + ";";

        List<IgdbGameDto> jeuxIgdb = igdbWebClient.post().uri("/games").bodyValue(body)
                .retrieve().bodyToFlux(IgdbGameDto.class)
                .collectList().defaultIfEmpty(List.of()).block();

        if (jeuxIgdb == null) return new ImportResult(0, 0);

        int importes = 0;
        for (IgdbGameDto dto : jeuxIgdb) {
            try {
                if (!jeuRepository.existsByExternalId(String.valueOf(dto.getId()))) {
                    jeuRepository.save(convertirDtoEnJeu(dto));
                    importes++;
                }
            } catch (Exception e) {
                log.error("Erreur import '{}' : {}", dto.getName(), e.getMessage());
            }
        }
        return new ImportResult(importes,
                (page + 1) * limit + (jeuxIgdb.size() < limit ? 0 : 9999));
    }

    // -------------------------------------------------------------------------
    // Import automatique complet
    // -------------------------------------------------------------------------

    public ImportProgress getProgress() { return progress; }

    public void lancerImportComplet() {
        if (progress.isRunning()) { log.warn("Import déjà en cours, ignoré."); return; }
        progress.setRunning(true);
        progress.setImported(0);
        progress.setSkipped(0);
        progress.setTotal(0);
        progress.setCurrentPage(0);
        progress.setDone(false);
        progress.setError(null);
        importAutoService.run();
    }

    // -------------------------------------------------------------------------
    // Création manuelle
    // -------------------------------------------------------------------------

    public JeuResponse creerJeuManuellement(JeuManuelRequest request) {
        Jeu jeu = new Jeu();
        jeu.setTitre(request.getTitre());
        jeu.setGenre(request.getGenre());
        jeu.setPlateforme(request.getPlateforme());
        jeu.setCoverUrl(request.getCoverUrl());
        jeu.setDescription(request.getDescription());
        jeu.setSource("manuel");
        jeu.setNoteMoyenne(0f);

        if (request.getDateSortie() != null && !request.getDateSortie().isBlank()) {
            try { jeu.setDateSortie(LocalDate.parse(request.getDateSortie())); }
            catch (Exception e) { log.warn("Date invalide : {}", request.getDateSortie()); }
        }
        return ResponseMapper.toJeuResponse(jeuRepository.save(jeu));
    }

    // -------------------------------------------------------------------------
    // Helpers privés
    // -------------------------------------------------------------------------

    private JeuResponse enrichWithStats(JeuResponse dto, Long jeuId) {
        long nb = jeuRepository.countBibliotheque(jeuId);
        dto.setNbBibliotheque(nb);

        List<Object[]> rows = jeuRepository.countParStatut(jeuId);
        Map<String, Long> statuts = new HashMap<>();
        for (Object[] row : rows) {
            statuts.put(row[0].toString(), (Long) row[1]);
        }
        dto.setStatutStats(statuts);
        return dto;
    }

    private Jeu importerEntite(Long igdbId) {
        String externalId = String.valueOf(igdbId);
        return jeuRepository.findByExternalId(externalId).orElseGet(() -> {
            String body = "fields name,summary,cover.url,genres.name,platforms.name,first_release_date;"
                    + " where id = " + igdbId + ";";
            IgdbGameDto dto = igdbWebClient.post().uri("/games").bodyValue(body)
                    .retrieve().bodyToFlux(IgdbGameDto.class).next().blockOptional()
                    .orElseThrow(() -> new RuntimeException("Jeu IGDB introuvable : " + igdbId));
            return jeuRepository.save(convertirDtoEnJeu(dto));
        });
    }

    private Jeu convertirDtoEnJeu(IgdbGameDto dto) {
        Jeu jeu = new Jeu();
        jeu.setTitre(dto.getName());
        jeu.setSource("igdb");
        jeu.setExternalId(String.valueOf(dto.getId()));
        jeu.setDescription(dto.getSummary());
        jeu.setNoteMoyenne(0f);

        if (dto.getCover() != null)
            jeu.setCoverUrl("https:" + dto.getCover().getUrl().replace("t_thumb", "t_cover_big"));
        if (dto.getGenres() != null && !dto.getGenres().isEmpty())
            jeu.setGenre(dto.getGenres().getFirst().getName());
        if (dto.getPlatforms() != null && !dto.getPlatforms().isEmpty())
            jeu.setPlateforme(dto.getPlatforms().getFirst().getName());
        if (dto.getFirstReleaseDate() != null)
            jeu.setDateSortie(Instant.ofEpochSecond(dto.getFirstReleaseDate())
                    .atZone(ZoneId.systemDefault()).toLocalDate());
        return jeu;
    }
    public void supprimerJeu(Long id) {
        Jeu jeu = jeuRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Jeu introuvable"));
        jeuRepository.delete(jeu);
    }
}