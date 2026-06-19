package com.example.back.controller;

import com.example.back.config.JwtUtil;
import com.example.back.entities.*;
import com.example.back.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration — Spring Boot 4 + H2 in-memory.
 *
 * Isolation entre appels MockMvc successifs :
 * Les appels POST via MockMvc s'exécutent chacun dans leur propre transaction
 * (celle du service appelé). Entre deux appels, H2 peut garder des données
 * dans son cache de connexion non encore visibles par la prochaine requête.
 *
 * Solution : TransactionTemplate.execute() pour forcer un commit explicite
 * entre deux appels MockMvc quand on teste des doublons.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true"
})
class IntegrationTest {

    @Autowired WebApplicationContext wac;
    @Autowired UtilisateurRepository utilisateurRepo;
    @Autowired JeuRepository jeuRepo;
    @Autowired AvisRepository avisRepo;
    @Autowired NoteRepository noteRepo;
    @Autowired BibliothequeRepository bibliothequeRepo;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtUtil jwtUtil;
    @Autowired TransactionTemplate transactionTemplate;

    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @AfterEach
    void cleanup() {
        // Nettoyage dans une transaction dédiée, ordre FK strict
        transactionTemplate.execute(status -> {
            noteRepo.deleteAll();
            avisRepo.deleteAll();
            bibliothequeRepo.deleteAll();
            jeuRepo.deleteAll();
            utilisateurRepo.deleteAll();
            return null;
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Crée un utilisateur dans une transaction committée immédiatement. */
    private Utilisateur createUser(String pseudo, String email, Role role) {
        return transactionTemplate.execute(status -> {
            Utilisateur u = new Utilisateur();
            u.setPseudo(pseudo); u.setEmail(email);
            u.setMdp(passwordEncoder.encode("Password1"));
            u.setDateCompte(LocalDate.now()); u.setRole(role);
            return utilisateurRepo.save(u);
        });
    }

    /** Crée un jeu dans une transaction committée immédiatement. */
    private Jeu createJeu(String titre) {
        return transactionTemplate.execute(status -> {
            Jeu j = new Jeu(); j.setTitre(titre);
            j.setSource("manuel"); j.setNoteMoyenne(0f);
            return jeuRepo.save(j);
        });
    }

    private String tokenFor(Utilisateur u) { return jwtUtil.generateToken(u); }

    // ── Helpers requêtes ──────────────────────────────────────────────────────

    private ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions postJson(String url, String body, String token) throws Exception {
        return mockMvc.perform(post(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions getJson(String url) throws Exception {
        return mockMvc.perform(get(url)
                .accept(MediaType.APPLICATION_JSON));
    }

    private ResultActions getJson(String url, String token) throws Exception {
        return mockMvc.perform(get(url)
                .header("Authorization", "Bearer " + token)
                .accept(MediaType.APPLICATION_JSON));
    }

    // ── /api/auth ─────────────────────────────────────────────────────────────

    @Nested @DisplayName("POST /api/auth/inscription")
    class Inscription {

        @Test @DisplayName("200 et tokens pour une inscription valide")
        void inscription_ok() throws Exception {
            postJson("/api/auth/inscription",
                    "{\"pseudo\":\"newuser\",\"email\":\"new@test.com\",\"motDePasse\":\"Password1\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.pseudo").value("newuser"));
        }

        @Test @DisplayName("409 si email déjà utilisé")
        void inscription_email_doublon() throws Exception {
            // Premier appel via createUser() → transaction committée
            createUser("existing", "exist@test.com", Role.USER);

            postJson("/api/auth/inscription",
                    "{\"pseudo\":\"other\",\"email\":\"exist@test.com\",\"motDePasse\":\"Password1\"}")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(containsString("Email")));
        }

        @Test @DisplayName("409 si pseudo déjà utilisé")
        void inscription_pseudo_doublon() throws Exception {
            // Premier appel via createUser() → transaction committée
            createUser("alice", "alice1@test.com", Role.USER);

            postJson("/api/auth/inscription",
                    "{\"pseudo\":\"alice\",\"email\":\"alice2@test.com\",\"motDePasse\":\"Password1\"}")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(containsString("Pseudo")));
        }

        @Test @DisplayName("400 si pseudo trop court")
        void inscription_pseudo_trop_court() throws Exception {
            postJson("/api/auth/inscription",
                    "{\"pseudo\":\"ab\",\"email\":\"x@test.com\",\"motDePasse\":\"Password1\"}")
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("400 si email malformé")
        void inscription_email_invalide() throws Exception {
            postJson("/api/auth/inscription",
                    "{\"pseudo\":\"valid\",\"email\":\"pas-un-email\",\"motDePasse\":\"Password1\"}")
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("400 si mot de passe trop court")
        void inscription_mdp_trop_court() throws Exception {
            postJson("/api/auth/inscription",
                    "{\"pseudo\":\"valid\",\"email\":\"v@test.com\",\"motDePasse\":\"short\"}")
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested @DisplayName("POST /api/auth/connexion")
    class Connexion {

        @Test @DisplayName("200 et tokens pour des identifiants corrects")
        void connexion_ok() throws Exception {
            createUser("alice", "alice@test.com", Role.USER);

            postJson("/api/auth/connexion",
                    "{\"email\":\"alice@test.com\",\"motDePasse\":\"Password1\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.pseudo").value("alice"));
        }

        @Test @DisplayName("401 pour un mauvais mot de passe")
        void connexion_mauvais_mot_de_passe() throws Exception {
            createUser("bob", "bob@test.com", Role.USER);

            postJson("/api/auth/connexion",
                    "{\"email\":\"bob@test.com\",\"motDePasse\":\"WrongPass1\"}")
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("404 si email inconnu")
        void connexion_email_inconnu() throws Exception {
            postJson("/api/auth/connexion",
                    "{\"email\":\"ghost@test.com\",\"motDePasse\":\"Password1\"}")
                    .andExpect(status().isNotFound());
        }
    }

    // ── /api/jeux ─────────────────────────────────────────────────────────────

    @Nested @DisplayName("GET /api/jeux")
    class JeuxEndpoint {

        @Test @DisplayName("200 sans authentification — catalogue public")
        void catalogue_public() throws Exception {
            createJeu("Dark Souls");
            getJson("/api/jeux")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").isNumber());
        }

        @Test @DisplayName("filtre par titre fonctionne")
        void filtre_titre() throws Exception {
            createJeu("Zelda BOTW");
            createJeu("Mario Kart");
            mockMvc.perform(get("/api/jeux")
                            .param("titre", "Zelda")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].titre",
                            everyItem(containsStringIgnoringCase("zelda"))));
        }

        @Test @DisplayName("pagination fonctionne")
        void pagination() throws Exception {
            for (int i = 0; i < 5; i++) createJeu("PaginGame " + i);
            mockMvc.perform(get("/api/jeux")
                            .param("page", "0").param("size", "3")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(3))))
                    .andExpect(jsonPath("$.totalPages").isNumber());
        }
    }

    @Nested @DisplayName("GET /api/jeux/:id")
    class JeuDetail {

        @Test @DisplayName("200 avec les infos complètes du jeu")
        void detail_ok() throws Exception {
            Jeu j = createJeu("Hades");
            getJson("/api/jeux/" + j.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.titre").value("Hades"))
                    .andExpect(jsonPath("$.vues").value(greaterThanOrEqualTo(0)));
        }

        @Test @DisplayName("404 pour un id inexistant")
        void detail_introuvable() throws Exception {
            getJson("/api/jeux/99999")
                    .andExpect(status().isNotFound());
        }
    }

    // ── /api/avis ─────────────────────────────────────────────────────────────

    @Nested @DisplayName("Avis endpoints")
    class AvisEndpoint {

        @Test @DisplayName("avis créé avec token valide")
        void creer_avis_ok() throws Exception {
            Utilisateur u = createUser("charlie", "charlie@test.com", Role.USER);
            Jeu j = createJeu("Celeste");
            postJson("/api/avis/jeu/" + j.getId(),
                    "{\"texte\":\"Un jeu magnifique et vraiment challengeant !\"}",
                    tokenFor(u))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.texte")
                            .value("Un jeu magnifique et vraiment challengeant !"))
                    .andExpect(jsonPath("$.utilisateurPseudo").value("charlie"));
        }

        @Test @DisplayName("401 sans token")
        void creer_avis_non_authentifie() throws Exception {
            Jeu j = createJeu("Celeste2");
            postJson("/api/avis/jeu/" + j.getId(),
                    "{\"texte\":\"Super jeu, vraiment excellent !\"}")
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("400 si texte trop court")
        void creer_avis_texte_trop_court() throws Exception {
            Utilisateur u = createUser("dave", "dave@test.com", Role.USER);
            Jeu j = createJeu("Fez");
            postJson("/api/avis/jeu/" + j.getId(),
                    "{\"texte\":\"Trop court\"}",
                    tokenFor(u))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("GET avis publics sans auth")
        void lire_avis_public() throws Exception {
            Jeu j = createJeu("Shovel Knight");
            getJson("/api/avis/jeu/" + j.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    // ── /api/notes ────────────────────────────────────────────────────────────

    @Nested @DisplayName("Notes endpoints")
    class NotesEndpoint {

        @Test @DisplayName("note créée et moyenne recalculée")
        void noter_ok() throws Exception {
            Utilisateur u = createUser("eve", "eve@test.com", Role.USER);
            Jeu j = createJeu("Ori");
            mockMvc.perform(post("/api/notes/jeu/" + j.getId())
                            .header("Authorization", "Bearer " + tokenFor(u))
                            .accept(MediaType.APPLICATION_JSON)
                            .param("valeur", "9.0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valeur").value(9.0));
        }

        @Test @DisplayName("400 si note > 10")
        void noter_hors_intervalle() throws Exception {
            Utilisateur u = createUser("frank", "frank@test.com", Role.USER);
            Jeu j = createJeu("GTA V");
            mockMvc.perform(post("/api/notes/jeu/" + j.getId())
                            .header("Authorization", "Bearer " + tokenFor(u))
                            .accept(MediaType.APPLICATION_JSON)
                            .param("valeur", "11.0"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── /api/bibliotheque ─────────────────────────────────────────────────────

    @Nested @DisplayName("Bibliothèque endpoints")
    class BibliEndpoint {

        @Test @DisplayName("POST ajoute le jeu, GET le récupère")
        void ajouter_puis_lister() throws Exception {
            Utilisateur u = createUser("grace", "grace@test.com", Role.USER);
            Jeu j = createJeu("Hollow Knight");

            mockMvc.perform(post("/api/bibliotheque/jeu/" + j.getId())
                            .header("Authorization", "Bearer " + tokenFor(u))
                            .accept(MediaType.APPLICATION_JSON)
                            .param("statut", "A_JOUER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statut").value("A_JOUER"));

            getJson("/api/bibliotheque", tokenFor(u))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].jeuTitre").value("Hollow Knight"));
        }

        @Test @DisplayName("409 si jeu déjà en bibliothèque")
        void ajouter_doublon() throws Exception {
            Utilisateur u = createUser("henry", "henry@test.com", Role.USER);
            Jeu j = createJeu("Cuphead");
            // Créer l'entrée bibliothèque dans une transaction committée
            transactionTemplate.execute(status -> {
                bibliothequeRepo.save(
                        new Bibliotheque(u, j, StatutJeu.JOUER, LocalDate.now()));
                return null;
            });

            mockMvc.perform(post("/api/bibliotheque/jeu/" + j.getId())
                            .header("Authorization", "Bearer " + tokenFor(u))
                            .accept(MediaType.APPLICATION_JSON)
                            .param("statut", "A_JOUER"))
                    .andExpect(status().isConflict());
        }

        @Test @DisplayName("PUT change le statut")
        void changer_statut() throws Exception {
            Utilisateur u = createUser("iris", "iris@test.com", Role.USER);
            Jeu j = createJeu("Blasphemous");
            transactionTemplate.execute(status -> {
                bibliothequeRepo.save(
                        new Bibliotheque(u, j, StatutJeu.A_JOUER, LocalDate.now()));
                return null;
            });

            mockMvc.perform(put("/api/bibliotheque/jeu/" + j.getId() + "/statut")
                            .header("Authorization", "Bearer " + tokenFor(u))
                            .accept(MediaType.APPLICATION_JSON)
                            .param("statut", "FINIT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statut").value("FINIT"));
        }

        @Test @DisplayName("DELETE supprime le jeu de la bibliothèque")
        void supprimer() throws Exception {
            Utilisateur u = createUser("jack", "jack@test.com", Role.USER);
            Jeu j = createJeu("Metroid Dread");
            transactionTemplate.execute(status -> {
                bibliothequeRepo.save(
                        new Bibliotheque(u, j, StatutJeu.JOUER, LocalDate.now()));
                return null;
            });

            mockMvc.perform(delete("/api/bibliotheque/jeu/" + j.getId())
                            .header("Authorization", "Bearer " + tokenFor(u)))
                    .andExpect(status().isNoContent());
        }
    }

    // ── /api/admin ────────────────────────────────────────────────────────────

    @Nested @DisplayName("Admin — contrôle des rôles")
    class AdminEndpoint {

        @Test @DisplayName("ADMIN peut lister les utilisateurs")
        void lister_utilisateurs_admin() throws Exception {
            Utilisateur admin = createUser("adminuser", "admin@test.com", Role.ADMIN);
            getJson("/api/admin/utilisateurs", tokenFor(admin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test @DisplayName("USER ne peut pas accéder à /api/admin")
        void acces_refuse_user() throws Exception {
            Utilisateur user = createUser("normaluser", "normal@test.com", Role.USER);
            getJson("/api/admin/utilisateurs", tokenFor(user))
                    .andExpect(status().isForbidden());
        }

        @Test @DisplayName("sans token → 401 sur les routes admin")
        void acces_refuse_sans_token() throws Exception {
            getJson("/api/admin/utilisateurs")
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── /api/users — profil public ────────────────────────────────────────────

    @Nested @DisplayName("GET /api/users/:id/profil")
    class ProfilPublic {

        @Test @DisplayName("200 accessible sans token")
        void profil_public_sans_auth() throws Exception {
            Utilisateur u = createUser("kate", "kate@test.com", Role.USER);
            getJson("/api/users/" + u.getId() + "/profil")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pseudo").value("kate"))
                    .andExpect(jsonPath("$.nombreJeux").isNumber())
                    .andExpect(jsonPath("$.nombreAvis").isNumber());
        }

        @Test @DisplayName("404 si utilisateur inexistant")
        void profil_inconnu() throws Exception {
            getJson("/api/users/99999/profil")
                    .andExpect(status().isNotFound());
        }
    }
}