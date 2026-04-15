package com.example.back.service;

import com.example.back.dto.IgdbGameDto;
import com.example.back.entities.Jeu;
import com.example.back.repository.JeuRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.ZoneId;

import java.time.Instant;
import java.util.List;

@Service
public class IgdbService {

    private final WebClient igdbWebClient;
    private final JeuRepository jeuRepository;

    public IgdbService(WebClient igdbWebClient,
                       JeuRepository jeuRepository) {
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
                .uri("/games")
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
                .blockFirst();

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
    public List<Jeu> listerJeux() {
        return jeuRepository.findAll();
    }
}