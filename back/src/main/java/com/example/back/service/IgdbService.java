package com.example.back.service;

import com.example.back.dto.IgdbGameDto;
import com.example.back.dto.ImportResult;
import com.example.back.entities.Jeu;
import com.example.back.repository.JeuRepository;
import org.springframework.beans.factory.annotation.Value;
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
    // Recherche live sur IGDB (pas de sauvegarde)
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
                .block();
    }

    // Importe un jeu IGDB → sauvegarde dans notre BDD
    public Jeu importerJeu(Long igdbId) {

        // Évite les doublons
        if (jeuRepository.existsByExternalId(
                String.valueOf(igdbId))) {
            return jeuRepository
                    .findAll().stream()
                    .filter(j -> String.valueOf(igdbId)
                            .equals(j.getExternalId()))
                    .findFirst().orElseThrow();
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

        Jeu jeu = new Jeu();
        jeu.setTitre(dto.getName());
        jeu.setSource("igdb");
        jeu.setExternalId(String.valueOf(igdbId));

        if (dto.getCover() != null) {
            String coverUrl = dto.getCover().getUrl()
                    .replace("t_thumb", "t_cover_big");
            jeu.setCoverUrl("https:" + coverUrl);
        }

        if (dto.getGenres() != null && !dto.getGenres().isEmpty()) {
            jeu.setGenre(dto.getGenres().get(0).getName());
        }

// Plateforme — on prend la première
        if (dto.getPlatforms() != null && !dto.getPlatforms().isEmpty()) {
            jeu.setPlateforme(dto.getPlatforms().get(0).getName());
        }

// Date de sortie — IGDB renvoie un timestamp Unix
        if (dto.getFirstReleaseDate() != null) {
            jeu.setDateSortie(Instant.ofEpochSecond(dto.getFirstReleaseDate())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate());
        }

        return jeuRepository.save(jeu);
    }
    public Page<Jeu> listerJeux(Pageable pageable) {
        return jeuRepository.findAll(pageable);
    }

    // Importer tous les jeux de l'API IGDB (batch import)
    public ImportResult importerJeuxPagines(int page, int limit) {
        int offset = page * limit;

        String body = "fields name,summary,cover.url,"
                + "genres.name,platforms.name,first_release_date;"
                + " limit " + limit + ";"
                + " offset " + offset + ";";

        List<IgdbGameDto> jeuxIgdb = igdbWebClient.post()
                .uri("/games")
                .bodyValue(body)   // ← utilise les defaultHeaders du WebClient, donc le token figé
                .retrieve()
                .bodyToFlux(IgdbGameDto.class)
                .collectList()
                .block();

        // Traitement et sauvegarde...
        List<Jeu> jeuxImportes = new ArrayList<>();

        for (IgdbGameDto dto : jeuxIgdb) {
            try {
                // Vérifier si le jeu n'existe pas déjà
                if (!jeuRepository.existsByExternalId(String.valueOf(dto.getId()))) {
                    Jeu jeu = convertirDtoEnJeu(dto);
                    jeuxImportes.add(jeuRepository.save(jeu));
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de l'import du jeu " + dto.getName() + ": " + e.getMessage());
            }
        }

// Estimation simple : on suppose qu'il y a au moins 5000 jeux au total
        int totalEstime = (page + 1) * limit + (jeuxIgdb.size() < limit ? 0 : 9999);

        return new ImportResult(jeuxImportes.size(), totalEstime);
    }
    private Jeu convertirDtoEnJeu(IgdbGameDto dto) {
        Jeu jeu = new Jeu();
        jeu.setTitre(dto.getName());
        jeu.setSource("igdb");
        jeu.setExternalId(String.valueOf(dto.getId()));

        if (dto.getCover() != null) {
            String coverUrl = dto.getCover().getUrl()
                    .replace("t_thumb", "t_cover_big");
            jeu.setCoverUrl("https:" + coverUrl);
        }

        if (dto.getGenres() != null && !dto.getGenres().isEmpty()) {
            jeu.setGenre(dto.getGenres().get(0).getName());
        }

        if (dto.getPlatforms() != null && !dto.getPlatforms().isEmpty()) {
            jeu.setPlateforme(dto.getPlatforms().get(0).getName());
        }

        if (dto.getFirstReleaseDate() != null) {
            jeu.setDateSortie(Instant.ofEpochSecond(dto.getFirstReleaseDate())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate());
        }

        return jeu;
    }
}