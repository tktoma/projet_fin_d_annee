package com.example.back.controller;

import com.example.back.exception.ConflictException;
import com.example.back.exception.GlobalExceptionHandler;
import com.example.back.exception.NotFoundException;
import com.example.back.exception.TokenExpiredException;
import com.example.back.service.UtilisateurService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ContextConfiguration(classes = {
        AuthController.class,
        GlobalExceptionHandlerTest.TestSecurityConfig.class,
        GlobalExceptionHandler.class
})
class GlobalExceptionHandlerTest {

    static final UtilisateurService SERVICE_MOCK = mock(UtilisateurService.class);

    @Configuration @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain chain(HttpSecurity http) throws Exception {
            http.csrf(c -> c.disable()).authorizeHttpRequests(a -> a.anyRequest().permitAll());
            return http.build();
        }
        @Bean UtilisateurService utilisateurService() { return SERVICE_MOCK; }
    }

    @Autowired MockMvc mockMvc;
    final ObjectMapper om = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach void resetMocks() { Mockito.reset(SERVICE_MOCK); }

    // ── Validation ───────────────────────────────────────────────────────────

    @Test @DisplayName("400 avec message lisible si email invalide")
    void bad_request_email_invalide() throws Exception {
        String body = "{\"pseudo\":\"alice\",\"email\":\"pas-un-email\",\"motDePasse\":\"monpass123\"}";
        mockMvc.perform(post("/api/auth/inscription").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test @DisplayName("400 si mot de passe trop court")
    void bad_request_password_court() throws Exception {
        String body = "{\"pseudo\":\"alice\",\"email\":\"a@a.com\",\"motDePasse\":\"court\"}";
        mockMvc.perform(post("/api/auth/inscription").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    // ── Exceptions métier ─────────────────────────────────────────────────────

    @Test @DisplayName("409 Conflict pour ConflictException")
    void conflict_exception() throws Exception {
        when(SERVICE_MOCK.inscrire(any())).thenThrow(new ConflictException("Email déjà utilisé"));
        String body = "{\"pseudo\":\"alice\",\"email\":\"a@a.com\",\"motDePasse\":\"monpass123\"}";
        mockMvc.perform(post("/api/auth/inscription").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email déjà utilisé"));
    }

    @Test @DisplayName("404 Not Found pour NotFoundException")
    void not_found_exception() throws Exception {
        when(SERVICE_MOCK.connecter(any())).thenThrow(new NotFoundException("Email introuvable"));
        String body = "{\"email\":\"x@x.com\",\"motDePasse\":\"monpass123\"}";
        mockMvc.perform(post("/api/auth/connexion").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Email introuvable"));
    }

    @Test @DisplayName("401 pour TokenExpiredException")
    void token_expired_exception() throws Exception {
        when(SERVICE_MOCK.refreshToken(any())).thenThrow(new TokenExpiredException("Refresh token expiré"));
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"tok\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token expiré"));
    }

    @Test @DisplayName("500 pour RuntimeException générique")
    void runtime_exception_donne_500() throws Exception {
        when(SERVICE_MOCK.connecter(any())).thenThrow(new RuntimeException("Erreur inattendue"));
        String body = "{\"email\":\"x@x.com\",\"motDePasse\":\"monpass123\"}";
        mockMvc.perform(post("/api/auth/connexion").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isInternalServerError());
    }

    @Test @DisplayName("La réponse d'erreur contient toujours message, status et timestamp")
    void structure_erreur_complete() throws Exception {
        when(SERVICE_MOCK.inscrire(any())).thenThrow(new ConflictException("Pseudo déjà utilisé"));
        String body = "{\"pseudo\":\"alice\",\"email\":\"a@a.com\",\"motDePasse\":\"monpass123\"}";
        mockMvc.perform(post("/api/auth/inscription").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}