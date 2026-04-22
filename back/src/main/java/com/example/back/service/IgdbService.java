package com.example.back.service;

import com.example.back.dto.IgdbGameDto;
import com.example.back.dto.ImportResult;
import com.example.back.dto.JeuResponse;
import com.example.back.dto.ResponseMapper;
import com.example.back.entities.Jeu;
import com.example.back.repository.JeuRepository;
import com.example.back.repository.JeuSpecification;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.ZoneId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class IgdbService {

    @Value("${igdb.client-id}")
    private String igdbClientId;

    private final WebClient igdbWebClient;
    private final JeuRepository jeuRepository;

    public IgdbService(WebClient igdbWebClient,
                       JeuRepository jeuRepository,
                       TwitchTokenService twitchTokenService) {
        this.igdbWebClient = igdbWebClient;
        this.jeuRepository = jeuRepository;
    }
    /**
     * Recherche live sur IGDB.
     * Le résultat est mis en cache 10 minutes par titre — évite
     * les appels répétés pour la même saisie et respecte la limite
     * de 4 req/sec de l'API IGDB.
     *
     * La clé est normalisée en minuscules pour que "Zelda" et "zelda"
     * partagent la même entrée de cache.
     */
    @Cacheable(
            value  = "recherches-igdb",
            key    = "#titre.toLowerCase().trim()",
            unless = "#result == null || #result.isEmpty()"
    )

    // Recherche live sur IGDB — inchangée
    public List<IgdbGameDto> rechercherJeu(String titre) {
        String body = "fields name,summary,cover.url,"
                + "genres.name,platforms.name,"
                + "first_release_date;"
                + " search \"" + titre + "\";"
                + " limit 10;";

        return igdbWebClient.post()
                .uri("/search")
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(IgdbGameDto.class)
                .collectList()
                .defaultIfEmpty(List.of())
                .block();
    }
    /**
     * Vide tout le cache des recherches IGDB.
     * Utile si les données IGDB ont changé et qu'on veut forcer
     * un rafraîchissement sans attendre l'expiration des 10 minutes.
     * Appelable via le endpoint d'admin si besoin.
     */
    @CacheEvict(value = "recherches-igdb", allEntries = true)
    public void viderCacheRecherches() {
        // Corps vide intentionnellement — l'annotation fait le travail
    }
    // Import d'un jeu unique — inchangé
    public JeuResponse importerJeu(Long igdbId) {
        return ResponseMapper.toJeuResponse(importerEntite(igdbId));
    }

    // Liste paginée sans filtre — inchangée, utilisée en interne
    public Page<JeuResponse> listerJeux(Pageable pageable) {
        return rechercherAvecFiltres(null, null, null, null, pageable);
    }

    /**
     * Recherche locale avec filtres combinés et optionnels.
     * Tous les paramètres peuvent être null — dans ce cas le filtre
     * est ignoré et on retourne tous les jeux.
     */
    public Page<JeuResponse> rechercherAvecFiltres(
            String titre,
            String genre,
            String plateforme,
            Float noteMin,
            Pageable pageable) {

        Specification<Jeu> spec = Specification
                .where(JeuSpecification.titreLike(titre))
                .and(JeuSpecification.genreEgal(genre))
                .and(JeuSpecification.plateformeEgale(plateforme))
                .and(JeuSpecification.noteMoyenneMin(noteMin));

        Page<Jeu> page = jeuRepository.findAll(spec, pageable);

        List<JeuResponse> dtos = page.getContent()
                .stream()
                .map(ResponseMapper::toJeuResponse)
                .toList();

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    // Import paginé depuis IGDB — inchangé
    public ImportResult importerJeuxPagines(int page, int limit) {
        int offset = page * limit;

        String body = "fields name,summary,cover.url,"
                + "genres.name,platforms.name,first_release_date;"
                + " limit " + limit + ";"
                + " offset " + offset + ";";

        List<IgdbGameDto> jeuxIgdb = igdbWebClient.post()
                .uri("/games")
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(IgdbGameDto.class)
                .collectList()
                .defaultIfEmpty(List.of())
                .block();

        List<Jeu> jeuxImportes = new ArrayList<>();
        for (IgdbGameDto dto : jeuxIgdb) {
            try {
                if (!jeuRepository.existsByExternalId(
                        String.valueOf(dto.getId()))) {
                    jeuxImportes.add(
                            jeuRepository.save(convertirDtoEnJeu(dto)));
                }
            } catch (Exception e) {
                System.err.println("Erreur import "
                        + dto.getName() + ": " + e.getMessage());
            }
        }

        int totalEstime = (page + 1) * limit
                + (jeuxIgdb.size() < limit ? 0 : 9999);
        return new ImportResult(jeuxImportes.size(), totalEstime);
    }

    // -------------------------------------------------------------------------
    // Méthodes internes
    // -------------------------------------------------------------------------

    private Jeu importerEntite(Long igdbId) {
        if (jeuRepository.existsByExternalId(String.valueOf(igdbId))) {
            return jeuRepository.findAll().stream()
                    .filter(j -> String.valueOf(igdbId)
                            .equals(j.getExternalId()))
                    .findFirst()
                    .orElseThrow();
        }

        String body = "fields name,summary,cover.url,"
                + "genres.name,platforms.name,"
                + "first_release_date;"
                + " where id = " + igdbId + ";";

        IgdbGameDto dto = igdbWebClient.post()
                .uri("/games")
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(IgdbGameDto.class)
                .next()
                .blockOptional()
                .orElseThrow(() -> new RuntimeException(
                        "Jeu IGDB introuvable : " + igdbId));

        return jeuRepository.save(convertirDtoEnJeu(dto));
    }

    private Jeu convertirDtoEnJeu(IgdbGameDto dto) {
        Jeu jeu = new Jeu();
        jeu.setTitre(dto.getName());
        jeu.setSource("igdb");
        jeu.setExternalId(String.valueOf(dto.getId()));

        if (dto.getCover() != null) {
            jeu.setCoverUrl("https:" + dto.getCover().getUrl()
                    .replace("t_thumb", "t_cover_big"));
        }
        if (dto.getGenres() != null && !dto.getGenres().isEmpty()) {
            jeu.setGenre(dto.getGenres().get(0).getName());
        }
        if (dto.getPlatforms() != null && !dto.getPlatforms().isEmpty()) {
            jeu.setPlateforme(dto.getPlatforms().get(0).getName());
        }
        if (dto.getFirstReleaseDate() != null) {
            jeu.setDateSortie(
                    Instant.ofEpochSecond(dto.getFirstReleaseDate())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate());
        }
        return jeu;
    }
}