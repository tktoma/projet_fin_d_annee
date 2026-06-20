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
 * IMPORTANT — PAS de @Nested ici.
 * Chaque classe @Nested dans un test @SpringBootTest force Spring à créer
 * un ApplicationContext DISTINCT par classe imbriquée (visible dans les logs :
 * "Found @SpringBootConfiguration ... for test class IntegrationTest$Inscription",
 * puis un AUTRE pour IntegrationTest$Connexion, etc.).
 * Comme application-test.properties utilise ddl-auto=create-drop sur une base
 * H2 PARTAGÉE (DB_CLOSE_DELAY=-1, même nom "testdb"), chaque nouveau contexte
 * DROP puis CREATE les tables au démarrage/arrêt — ce qui efface les données
 * du test précédent ou laisse les tables absentes pendant l'exécution d'un
 * autre test du même run. D'où les "Body vide" et "200 au lieu de 409" qui
 * apparaissaient de façon instable selon l'ordre d'exécution des contextes.
 *
 * Solution : une seule classe plate, un seul ApplicationContext pour
 * l'ensemble de la suite, méthodes nommées avec préfixe de groupe.
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
        transactionTemplate.execute(status -> {
            noteRepo.deleteAll();
            avisRepo.deleteAll();
            bibliothequeRepo.deleteAll();
            jeuRepo.deleteAll();
            utilisateurRepo.deleteAll();
            return null;
        });
    }

    // ── Helpers data ──────────────────────────────────────────────────────────

    private Utilisateur createUser(String pseudo, String email, Role role) {
        return transactionTemplate.execute(status -> {
            Utilisateur u = new Utilisateur();
            u.setPseudo(pseudo); u.setEmail(email);
            u.setMdp(passwordEncoder.encode("Password1"));
            u.setDateCompte(LocalDate.now()); u.setRole(role);
            return utilisateurRepo.save(u);
        });
    }

    private Jeu createJeu(String titre) {
        return transactionTemplate.execute(status -> {
            Jeu j = new Jeu(); j.setTitre(titre);
            j.setSource("manuel"); j.setNoteMoyenne(0f);
            return jeuRepo.save(j);
        });
    }

    private void createBibliotheque(Utilisateur u, Jeu j, StatutJeu statut) {
        transactionTemplate.execute(status -> {
            bibliothequeRepo.save(new Bibliotheque(u, j, statut, LocalDate.now()));
            return null;
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
        return mockMvc.perform(get(url).accept(MediaType.APPLICATION_JSON));
    }

    private ResultActions getJson(String url, String token) throws Exception {
        return mockMvc.perform(get(url)
                .header("Authorization", "Bearer " + token)
                .accept(MediaType.APPLICATION_JSON));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // POST /api/auth/inscription
    // ═════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("Inscription : 200 et tokens pour une inscription valide")
    void inscription_200_ok() throws Exception {
        postJson("/api/auth/inscription",
                "{\"pseudo\":\"newuser\",\"email\":\"new@test.com\",\"motDePasse\":\"Password1\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.pseudo").value("newuser"));
    }

    @Test @DisplayName("Inscription : 409 si email déjà utilisé")
    void inscription_409_email_doublon() throws Exception {
        createUser("existing", "exist@test.com", Role.USER);

        postJson("/api/auth/inscription",
                "{\"pseudo\":\"other\",\"email\":\"exist@test.com\",\"motDePasse\":\"Password1\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Email")));
    }

    @Test @DisplayName("Inscription : 409 si pseudo déjà utilisé")
    void inscription_409_pseudo_doublon() throws Exception {
        createUser("alice", "alice1@test.com", Role.USER);

        postJson("/api/auth/inscription",
                "{\"pseudo\":\"alice\",\"email\":\"alice2@test.com\",\"motDePasse\":\"Password1\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Pseudo")));
    }

    @Test @DisplayName("Inscription : 400 si pseudo trop court")
    void inscription_400_pseudo_trop_court() throws Exception {
        postJson("/api/auth/inscription",
                "{\"pseudo\":\"ab\",\"email\":\"x@test.com\",\"motDePasse\":\"Password1\"}")
                .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("Inscription : 400 si email malformé")
    void inscription_400_email_invalide() throws Exception {
        postJson("/api/auth/inscription",
                "{\"pseudo\":\"valid\",\"email\":\"pas-un-email\",\"motDePasse\":\"Password1\"}")
                .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("Inscription : 400 si mot de passe trop court")
    void inscription_400_mdp_trop_court() throws Exception {
        postJson("/api/auth/inscription",
                "{\"pseudo\":\"valid\",\"email\":\"v@test.com\",\"motDePasse\":\"short\"}")
                .andExpect(status().isBadRequest());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // POST /api/auth/connexion
    // ═════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("Connexion : 200 et tokens pour des identifiants corrects")
    void connexion_200_ok() throws Exception {
        createUser("alice", "alice@test.com", Role.USER);

        postJson("/api/auth/connexion",
                "{\"email\":\"alice@test.com\",\"motDePasse\":\"Password1\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.pseudo").value("alice"));
    }

    @Test @DisplayName("Connexion : 401 pour un mauvais mot de passe")
    void connexion_401_mauvais_mot_de_passe() throws Exception {
        createUser("bob", "bob@test.com", Role.USER);

        postJson("/api/auth/connexion",
                "{\"email\":\"bob@test.com\",\"motDePasse\":\"WrongPass1\"}")
                .andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("Connexion : 404 si email inconnu")
    void connexion_404_email_inconnu() throws Exception {
        postJson("/api/auth/connexion",
                "{\"email\":\"ghost@test.com\",\"motDePasse\":\"Password1\"}")
                .andExpect(status().isNotFound());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /api/jeux
    // ═════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("Jeux : 200 catalogue public sans authentification")
    void jeux_200_catalogue_public() throws Exception {
        createJeu("Dark Souls");
        getJson("/api/jeux")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test @DisplayName("Jeux : filtre par titre fonctionne")
    void jeux_filtre_titre() throws Exception {
        createJeu("Zelda BOTW");
        createJeu("Mario Kart");
        mockMvc.perform(get("/api/jeux")
                        .param("titre", "Zelda")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].titre",
                        everyItem(containsStringIgnoringCase("zelda"))));
    }

    @Test @DisplayName("Jeux : pagination fonctionne")
    void jeux_pagination() throws Exception {
        for (int i = 0; i < 5; i++) createJeu("PaginGame " + i);
        mockMvc.perform(get("/api/jeux")
                        .param("page", "0").param("size", "3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(3))))
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /api/jeux/:id
    // ═════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("Jeu détail : 200 avec les infos complètes")
    void jeuDetail_200_ok() throws Exception {
        Jeu j = createJeu("Hades");
        getJson("/api/jeux/" + j.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titre").value("Hades"))
                .andExpect(jsonPath("$.vues").value(greaterThanOrEqualTo(0)));
    }

    @Test @DisplayName("Jeu détail : 404 pour un id inexistant")
    void jeuDetail_404_introuvable() throws Exception {
        getJson("/api/jeux/99999")
                .andExpect(status().isNotFound());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // /api/avis
    // ═════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("Avis : créé avec token valide")
    void avis_200_creer_ok() throws Exception {
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

    @Test @DisplayName("Avis : 401 sans token")
    void avis_401_non_authentifie() throws Exception {
        Jeu j = createJeu("Celeste2");
        postJson("/api/avis/jeu/" + j.getId(),
                "{\"texte\":\"Super jeu, vraiment excellent !\"}")
                .andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("Avis : 400 si texte trop court")
    void avis_400_texte_trop_court() throws Exception {
        Utilisateur u = createUser("dave", "dave@test.com", Role.USER);
        Jeu j = createJeu("Fez");
        postJson("/api/avis/jeu/" + j.getId(),
                "{\"texte\":\"Court\"}",
                tokenFor(u))
                .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("Avis : GET liste publique sans auth")
    void avis_200_lire_public() throws Exception {
        Jeu j = createJeu("Shovel Knight");
        getJson("/api/avis/jeu/" + j.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // /api/notes
    // ═════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("Notes : note créée et moyenne recalculée")
    void notes_200_noter_ok() throws Exception {
        Utilisateur u = createUser("eve", "eve@test.com", Role.USER);
        Jeu j = createJeu("Ori");
        mockMvc.perform(post("/api/notes/jeu/" + j.getId())
                        .header("Authorization", "Bearer " + tokenFor(u))
                        .accept(MediaType.APPLICATION_JSON)
                        .param("valeur", "9.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valeur").value(9.0));
    }

    @Test @DisplayName("Notes : 400 si note > 10")
    void notes_400_hors_intervalle() throws Exception {
        Utilisateur u = createUser("frank", "frank@test.com", Role.USER);
        Jeu j = createJeu("GTA V");
        mockMvc.perform(post("/api/notes/jeu/" + j.getId())
                        .header("Authorization", "Bearer " + tokenFor(u))
                        .accept(MediaType.APPLICATION_JSON)
                        .param("valeur", "11.0"))
                .andExpect(status().isBadRequest());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // /api/bibliotheque
    // ═════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("Bibliothèque : POST ajoute le jeu, GET le récupère")
    void biblio_200_ajouter_puis_lister() throws Exception {
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

    @Test @DisplayName("Bibliothèque : 409 si jeu déjà présent")
    void biblio_409_doublon() throws Exception {
        Utilisateur u = createUser("henry", "henry@test.com", Role.USER);
        Jeu j = createJeu("Cuphead");
        createBibliotheque(u, j, StatutJeu.JOUER);

        mockMvc.perform(post("/api/bibliotheque/jeu/" + j.getId())
                        .header("Authorization", "Bearer " + tokenFor(u))
                        .accept(MediaType.APPLICATION_JSON)
                        .param("statut", "A_JOUER"))
                .andExpect(status().isConflict());
    }

    @Test @DisplayName("Bibliothèque : PUT change le statut")
    void biblio_200_changer_statut() throws Exception {
        Utilisateur u = createUser("iris", "iris@test.com", Role.USER);
        Jeu j = createJeu("Blasphemous");
        createBibliotheque(u, j, StatutJeu.A_JOUER);

        mockMvc.perform(put("/api/bibliotheque/jeu/" + j.getId() + "/statut")
                        .header("Authorization", "Bearer " + tokenFor(u))
                        .accept(MediaType.APPLICATION_JSON)
                        .param("statut", "FINIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("FINIT"));
    }

    @Test @DisplayName("Bibliothèque : DELETE supprime le jeu")
    void biblio_204_supprimer() throws Exception {
        Utilisateur u = createUser("jack", "jack@test.com", Role.USER);
        Jeu j = createJeu("Metroid Dread");
        createBibliotheque(u, j, StatutJeu.JOUER);

        mockMvc.perform(delete("/api/bibliotheque/jeu/" + j.getId())
                        .header("Authorization", "Bearer " + tokenFor(u)))
                .andExpect(status().isNoContent());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // /api/admin
    // ═════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("Admin : ADMIN peut lister les utilisateurs")
    void admin_200_lister_utilisateurs() throws Exception {
        Utilisateur admin = createUser("adminuser", "admin@test.com", Role.ADMIN);
        getJson("/api/admin/utilisateurs", tokenFor(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test @DisplayName("Admin : 403 si USER tente d'accéder")
    void admin_403_acces_refuse_user() throws Exception {
        Utilisateur user = createUser("normaluser", "normal@test.com", Role.USER);
        getJson("/api/admin/utilisateurs", tokenFor(user))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("Admin : 401 sans token")
    void admin_401_sans_token() throws Exception {
        getJson("/api/admin/utilisateurs")
                .andExpect(status().isUnauthorized());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // /api/users — profil public
    // ═════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("Profil public : 200 accessible sans token")
    void profil_200_public_sans_auth() throws Exception {
        Utilisateur u = createUser("kate", "kate@test.com", Role.USER);
        getJson("/api/users/" + u.getId() + "/profil")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pseudo").value("kate"))
                .andExpect(jsonPath("$.nombreJeux").isNumber())
                .andExpect(jsonPath("$.nombreAvis").isNumber());
    }

    @Test @DisplayName("Profil public : 404 si utilisateur inexistant")
    void profil_404_inconnu() throws Exception {
        getJson("/api/users/99999/profil")
                .andExpect(status().isNotFound());
    }
}