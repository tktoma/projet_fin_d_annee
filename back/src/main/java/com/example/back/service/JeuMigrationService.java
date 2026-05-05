package com.example.back.service;

import com.example.back.dto.IgdbGameDto;
import com.example.back.entities.Jeu;
import com.example.back.repository.JeuRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class JeuMigrationService {

    private static final Logger log = LoggerFactory.getLogger(JeuMigrationService.class);

    private final JeuRepository jeuRepository;
    private final WebClient igdbWebClient;

    public JeuMigrationService(JeuRepository jeuRepository,
                               WebClient igdbWebClient) {
        this.jeuRepository = jeuRepository;
        this.igdbWebClient = igdbWebClient;
    }

    /**
     * Met à jour la description de tous les jeux qui n'en ont pas encore.
     * Appel en batch de 10 IDs à la fois pour limiter les requêtes IGDB.
     */
    @Async
    public void enrichirDescriptions() {
        List<Jeu> jeuxSansDescription = jeuRepository.findAll()
                .stream()
                .filter(j -> j.getDescription() == null || j.getDescription().isBlank())
                .filter(j -> j.getExternalId() != null)
                .toList();

        log.info("Enrichissement descriptions : {} jeux à mettre à jour", jeuxSansDescription.size());

        int batchSize = 10;
        for (int i = 0; i < jeuxSansDescription.size(); i += batchSize) {
            List<Jeu> batch = jeuxSansDescription.subList(i, Math.min(i + batchSize, jeuxSansDescription.size()));
            String ids = batch.stream()
                    .map(Jeu::getExternalId)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

            String body = "fields id,summary; where id = (" + ids + ");";

            try {
                List<IgdbGameDto> dtos = igdbWebClient.post()
                        .uri("/games")
                        .bodyValue(body)
                        .retrieve()
                        .bodyToFlux(IgdbGameDto.class)
                        .collectList()
                        .block();

                if (dtos == null) continue;

                for (IgdbGameDto dto : dtos) {
                    if (dto.getSummary() == null) continue;
                    batch.stream()
                            .filter(j -> j.getExternalId().equals(String.valueOf(dto.getId())))
                            .findFirst()
                            .ifPresent(j -> {
                                j.setDescription(dto.getSummary());
                                jeuRepository.save(j);
                                log.debug("Description mise à jour : {}", j.getTitre());
                            });
                }
            } catch (Exception e) {
                log.error("Erreur batch descriptions (offset {}) : {}", i, e.getMessage());
            }
        }

        log.info("Enrichissement descriptions terminé");
    }
}