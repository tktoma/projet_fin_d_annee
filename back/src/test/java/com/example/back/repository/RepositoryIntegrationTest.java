package com.example.back.repository;

import com.example.back.entities.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests d'intégration JPA sur base H2 in-memory.
 * Utilisent @SpringBootTest (Spring Boot 4 — @DataJpaTest a déménagé dans
 * un module séparé non inclus dans ce projet).
 * Chaque test est @Transactional → rollback automatique.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class RepositoryIntegrationTest {

    @Autowired UtilisateurRepository utilisateurRepo;
    @Autowired JeuRepository jeuRepo;
    @Autowired AvisRepository avisRepo;
    @Autowired NoteRepository noteRepo;
    @Autowired BibliothequeRepository bibliothequeRepo;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Utilisateur saveUser(String pseudo, String email) {
        Utilisateur u = new Utilisateur();
        u.setPseudo(pseudo); u.setEmail(email); u.setMdp("hashed");
        u.setDateCompte(LocalDate.now()); u.setRole(Role.USER);
        return utilisateurRepo.save(u);
    }

    private Jeu saveJeu(String titre, String externalId) {
        Jeu j = new Jeu(); j.setTitre(titre); j.setSource("igdb");
        j.setExternalId(externalId); j.setNoteMoyenne(0f);
        return jeuRepo.save(j);
    }

    // ── UtilisateurRepository ─────────────────────────────────────────────────

    @Nested @DisplayName("UtilisateurRepository")
    class UtilisateurRepoTests {

        @Test @DisplayName("findByEmail() retourne l'utilisateur correspondant")
        void find_by_email() {
            saveUser("alice", "alice@test.com");
            Optional<Utilisateur> found = utilisateurRepo.findByEmail("alice@test.com");
            assertThat(found).isPresent();
            assertThat(found.get().getPseudo()).isEqualTo("alice");
        }

        @Test @DisplayName("existsByEmail() retourne false si inexistant")
        void exists_by_email_false() {
            assertThat(utilisateurRepo.existsByEmail("ghost@test.com")).isFalse();
        }

        @Test @DisplayName("existsByPseudo() retourne true si présent")
        void exists_by_pseudo_true() {
            saveUser("bob", "bob@test.com");
            assertThat(utilisateurRepo.existsByPseudo("bob")).isTrue();
        }

        @Test @DisplayName("findByRefreshToken() localise par token")
        void find_by_refresh_token() {
            Utilisateur u = saveUser("carol", "carol@test.com");
            u.setRefreshToken("token-xyz");
            utilisateurRepo.save(u);
            Optional<Utilisateur> found = utilisateurRepo.findByRefreshToken("token-xyz");
            assertThat(found).isPresent();
            assertThat(found.get().getPseudo()).isEqualTo("carol");
        }
    }

    // ── JeuRepository ─────────────────────────────────────────────────────────

    @Nested @DisplayName("JeuRepository")
    class JeuRepoTests {

        @Test @DisplayName("existsByExternalId() détecte un jeu existant")
        void exists_by_external_id() {
            saveJeu("Zelda", "ext-001");
            assertThat(jeuRepo.existsByExternalId("ext-001")).isTrue();
        }

        @Test @DisplayName("findByTitreContainingIgnoreCase() fonctionne en insensible à la casse")
        void find_by_titre_ignore_case() {
            saveJeu("Dark Souls III", "ext-002");
            List<Jeu> results = jeuRepo.findByTitreContainingIgnoreCase("dark souls");
            assertThat(results).isNotEmpty();
            assertThat(results.get(0).getTitre()).containsIgnoringCase("dark souls");
        }

        @Test @DisplayName("findDistinctGenres() liste les genres distincts non nuls")
        void find_distinct_genres() {
            Jeu j1 = saveJeu("Game1", "ext-g1"); j1.setGenre("RPG"); jeuRepo.save(j1);
            Jeu j2 = saveJeu("Game2", "ext-g2"); j2.setGenre("Action"); jeuRepo.save(j2);
            Jeu j3 = saveJeu("Game3", "ext-g3"); j3.setGenre("RPG"); jeuRepo.save(j3);
            List<String> genres = jeuRepo.findDistinctGenres();
            assertThat(genres).containsExactlyInAnyOrder("Action", "RPG");
        }

        @Test @DisplayName("countBibliotheque() compte les entrées pour un jeu donné")
        void count_bibliotheque() {
            Utilisateur u1 = saveUser("u1", "u1@t.com");
            Utilisateur u2 = saveUser("u2", "u2@t.com");
            Jeu j = saveJeu("Elden Ring", "ext-er");
            bibliothequeRepo.save(new Bibliotheque(u1, j, StatutJeu.FINIT, LocalDate.now()));
            bibliothequeRepo.save(new Bibliotheque(u2, j, StatutJeu.JOUER, LocalDate.now()));
            assertThat(jeuRepo.countBibliotheque(j.getId())).isEqualTo(2L);
        }
    }

    // ── AvisRepository ────────────────────────────────────────────────────────

    @Nested @DisplayName("AvisRepository")
    class AvisRepoTests {

        @Test @DisplayName("findByUtilisateurIdAndJeuId() localise l'avis unique")
        void find_by_user_and_jeu() {
            Utilisateur u = saveUser("dave", "dave@t.com");
            Jeu j = saveJeu("Hades", "ext-h");
            Avis a = Avis.builder().utilisateur(u).jeu(j).texte("Excellent !").likes(0)
                    .dislikes(0).date(LocalDate.now()).build();
            avisRepo.save(a);
            Optional<Avis> found = avisRepo.findByUtilisateurIdAndJeuId(u.getId(), j.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getTexte()).isEqualTo("Excellent !");
        }

        @Test @DisplayName("countByUtilisateurId() retourne le bon compteur")
        void count_by_utilisateur() {
            Utilisateur u = saveUser("eve", "eve@t.com");
            Jeu j1 = saveJeu("Game A", "ext-a");
            Jeu j2 = saveJeu("Game B", "ext-b");
            avisRepo.save(Avis.builder().utilisateur(u).jeu(j1).texte("bien").likes(0).dislikes(0).date(LocalDate.now()).build());
            avisRepo.save(Avis.builder().utilisateur(u).jeu(j2).texte("super").likes(0).dislikes(0).date(LocalDate.now()).build());
            assertThat(avisRepo.countByUtilisateurId(u.getId())).isEqualTo(2L);
        }
    }

    // ── NoteRepository ────────────────────────────────────────────────────────

    @Nested @DisplayName("NoteRepository")
    class NoteRepoTests {

        @Test @DisplayName("calculerMoyenne() retourne la moyenne exacte")
        void calculer_moyenne() {
            Utilisateur u1 = saveUser("f1", "f1@t.com");
            Utilisateur u2 = saveUser("f2", "f2@t.com");
            Jeu j = saveJeu("Celeste", "ext-c");
            Note n1 = new Note(); n1.setUtilisateur(u1); n1.setJeu(j); n1.setValeur(8f); n1.setDate(LocalDate.now());
            Note n2 = new Note(); n2.setUtilisateur(u2); n2.setJeu(j); n2.setValeur(10f); n2.setDate(LocalDate.now());
            noteRepo.save(n1); noteRepo.save(n2);
            Optional<Float> avg = noteRepo.calculerMoyenne(j.getId());
            assertThat(avg).isPresent();
            assertThat(avg.get()).isEqualTo(9f);
        }

        @Test @DisplayName("calculerMoyenne() retourne empty si aucune note")
        void calculer_moyenne_vide() {
            Jeu j = saveJeu("Fez", "ext-fez");
            assertThat(noteRepo.calculerMoyenne(j.getId())).isEmpty();
        }
    }

    // ── BibliothequeRepository ────────────────────────────────────────────────

    @Nested @DisplayName("BibliothequeRepository")
    class BibliorepoTests {

        @Test @DisplayName("findByUtilisateurIdAndStatut() filtre par statut")
        void find_by_statut() {
            Utilisateur u = saveUser("g1", "g1@t.com");
            Jeu j1 = saveJeu("GA", "ext-ga");
            Jeu j2 = saveJeu("GB", "ext-gb");
            bibliothequeRepo.save(new Bibliotheque(u, j1, StatutJeu.JOUER, LocalDate.now()));
            bibliothequeRepo.save(new Bibliotheque(u, j2, StatutJeu.A_JOUER, LocalDate.now()));
            List<Bibliotheque> jouer = bibliothequeRepo.findByUtilisateurIdAndStatut(u.getId(), StatutJeu.JOUER);
            assertThat(jouer).hasSize(1);
            assertThat(jouer.get(0).getJeu().getTitre()).isEqualTo("GA");
        }
    }
}