package com.example.back.service;

import com.example.back.dto.IgdbGameDto;
import com.example.back.dto.ImportProgress;
import com.example.back.repository.JeuRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import com.example.back.entities.Jeu;

@Service
public class ImportAutoService {

    private static final Logger log = LoggerFactory.getLogger(ImportAutoService.class);

    private final WebClient igdbWebClient;
    private final JeuRepository jeuRepository;
    private final ImportProgress progress;

    public ImportAutoService(WebClient igdbWebClient,
                             JeuRepository jeuRepository,
                             ImportProgress progress) {
        this.igdbWebClient = igdbWebClient;
        this.jeuRepository = jeuRepository;
        this.progress = progress;
    }

    @Async
    public void run() {
        final int limit = 500;
        int pageNum = 0;

        log.info("Import automatique complet démarré");
        try {
            while (true) {
                int offset = pageNum * limit;
                String body = "fields name,summary,cover.url,"
                        + "genres.name,platforms.name,first_release_date;"
                        + " limit " + limit + "; offset " + offset + ";";

                List<IgdbGameDto> jeuxIgdb;
                try {
                    jeuxIgdb = igdbWebClient.post().uri("/games").bodyValue(body)
                            .retrieve().bodyToFlux(IgdbGameDto.class)
                            .collectList().defaultIfEmpty(List.of()).block();
                } catch (Exception e) {
                    log.error("Erreur IGDB page {} : {}", pageNum, e.getMessage());
                    progress.setError("Erreur IGDB page " + pageNum + " : " + e.getMessage());
                    break;
                }

                if (jeuxIgdb == null || jeuxIgdb.isEmpty()) {
                    log.info("Plus de jeux à la page {}, import terminé.", pageNum);
                    break;
                }

                progress.setCurrentPage(pageNum);

                for (IgdbGameDto dto : jeuxIgdb) {
                    try {
                        String externalId = String.valueOf(dto.getId());
                        if (jeuRepository.existsByExternalId(externalId)) {
                            progress.setSkipped(progress.getSkipped() + 1);
                        } else {
                            jeuRepository.save(toJeu(dto));
                            progress.setImported(progress.getImported() + 1);
                        }
                        progress.setTotal(progress.getTotal() + 1);
                    } catch (Exception e) {
                        log.error("Erreur import '{}' : {}", dto.getName(), e.getMessage());
                    }
                }

                log.info("Page {} : {} importés, {} ignorés (total : {})",
                        pageNum, progress.getImported(), progress.getSkipped(), progress.getTotal());

                if (jeuxIgdb.size() < limit) break;

                pageNum++;
                try { Thread.sleep(250); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        } finally {
            progress.setRunning(false);
            progress.setDone(true);
            log.info("Import terminé — {} importés, {} ignorés, {} total",
                    progress.getImported(), progress.getSkipped(), progress.getTotal());
        }
    }

    private Jeu toJeu(IgdbGameDto dto) {
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
}