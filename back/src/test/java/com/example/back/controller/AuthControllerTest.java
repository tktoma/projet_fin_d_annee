package com.example.back.controller;

import com.example.back.dto.AuthResponse;
import com.example.back.dto.LoginRequest;
import com.example.back.dto.RegisterRequest;
import com.example.back.exception.ConflictException;
import com.example.back.exception.GlobalExceptionHandler;
import com.example.back.exception.NotFoundException;
import com.example.back.service.UtilisateurService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@ContextConfiguration(classes = {
        AuthController.class,
        AuthControllerTest.TestSecurityConfig.class,
        GlobalExceptionHandler.class
})
class AuthControllerTest {

    // Instance Mockito statique — créée une fois, partagée entre
    // TestSecurityConfig (qui l'expose comme @Bean) et les tests
    // (qui configurent les when() dessus directement)
    static final UtilisateurService SERVICE_MOCK =
            mock(UtilisateurService.class);

    @Configuration
    @EnableWebSecurity
    static class TestSecurityConfig {

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http)
                throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth ->
                            auth.anyRequest().permitAll());
            return http.build();
        }

        // Retourne exactement la même instance que SERVICE_MOCK
        // → Spring injecte ce bean dans AuthController
        // → les tests configurent when() sur ce même objet
        @Bean
        public UtilisateurService utilisateurService() {
            return SERVICE_MOCK;
        }
    }

    @Autowired MockMvc mockMvc;

    // ObjectMapper instancié directement — pas besoin de l'autowirer
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Reset les stubs entre chaque test pour éviter les interférences
    @BeforeEach
    void resetMock() {
        reset(SERVICE_MOCK);
    }

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private AuthResponse authResponse() {
        return new AuthResponse("jwt-token", "refresh-token", "alice", 1L);
    }

    private RegisterRequest registerRequest() {
        RegisterRequest r = new RegisterRequest();
        r.setPseudo("alice");
        r.setEmail("alice@example.com");
        r.setMotDePasse("motdepasse123");
        return r;
    }

    private LoginRequest loginRequest() {
        LoginRequest r = new LoginRequest();
        r.setEmail("alice@example.com");
        r.setMotDePasse("motdepasse123");
        return r;
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/inscription
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/auth/inscription")
    class Inscription {

        @Test
        @DisplayName("200 et AuthResponse quand la requête est valide")
        void inscription_200() throws Exception {
            when(SERVICE_MOCK.inscrire(any())).thenReturn(authResponse());

            mockMvc.perform(post("/api/auth/inscription")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper
                                    .writeValueAsString(registerRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.pseudo").value("alice"))
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("400 si le pseudo est vide")
        void inscription_400_pseudoVide() throws Exception {
            RegisterRequest invalid = registerRequest();
            invalid.setPseudo("");

            mockMvc.perform(post("/api/auth/inscription")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("400 si l'email est malformé")
        void inscription_400_emailInvalide() throws Exception {
            RegisterRequest invalid = registerRequest();
            invalid.setEmail("pas-un-email");

            mockMvc.perform(post("/api/auth/inscription")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("400 si le mot de passe est trop court")
        void inscription_400_motDePasseTropCourt() throws Exception {
            RegisterRequest invalid = registerRequest();
            invalid.setMotDePasse("court");

            mockMvc.perform(post("/api/auth/inscription")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("500 si le service lève une RuntimeException")
        void inscription_500_emailDejaPris() throws Exception {
            when(SERVICE_MOCK.inscrire(any()))
                    .thenThrow(new ConflictException("Email déjà utilisé")); // ← exception métier

            mockMvc.perform(post("/api/auth/inscription")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest())))
                    .andExpect(status().isConflict())          // ← 409 maintenant cohérent
                    .andExpect(jsonPath("$.message").value("Email déjà utilisé"));
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/connexion
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/auth/connexion")
    class Connexion {

        @Test
        @DisplayName("200 et AuthResponse quand les identifiants sont corrects")
        void connexion_200() throws Exception {
            when(SERVICE_MOCK.connecter(any())).thenReturn(authResponse());

            mockMvc.perform(post("/api/auth/connexion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper
                                    .writeValueAsString(loginRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token"))
                    .andExpect(jsonPath("$.pseudo").value("alice"));
        }

        @Test
        @DisplayName("400 si l'email est vide")
        void connexion_400_emailVide() throws Exception {
            LoginRequest invalid = loginRequest();
            invalid.setEmail("");

            mockMvc.perform(post("/api/auth/connexion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("400 si le mot de passe est vide")
        void connexion_400_motDePasseVide() throws Exception {
            LoginRequest invalid = loginRequest();
            invalid.setMotDePasse("");

            mockMvc.perform(post("/api/auth/connexion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("500 si le service lève une RuntimeException")
        void connexion_500_mauvaisMotDePasse() throws Exception {
            when(SERVICE_MOCK.connecter(any()))
                    .thenThrow(new NotFoundException("Mot de passe incorrect"));

            mockMvc.perform(post("/api/auth/connexion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper
                                    .writeValueAsString(loginRequest())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value("Mot de passe incorrect"));
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/refresh
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("200 et nouveaux tokens quand le refresh token est valide")
        void refresh_200() throws Exception {
            when(SERVICE_MOCK.refreshToken("refresh-token"))
                    .thenReturn(new AuthResponse(
                            "nouveau-jwt", "nouveau-refresh", "alice", 1L));

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"refresh-token\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("nouveau-jwt"))
                    .andExpect(jsonPath("$.refreshToken")
                            .value("nouveau-refresh"));
        }

        @Test
        @DisplayName("500 si le refresh token est expiré ou invalide")
        void refresh_500_tokenExpire() throws Exception {
            when(SERVICE_MOCK.refreshToken(any()))
                    .thenThrow(new RuntimeException("Refresh token expiré"));

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"token-expire\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message")
                            .value("Refresh token expiré"));
        }
    }
}