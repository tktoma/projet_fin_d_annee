package com.example.back.config;

import com.example.back.entities.Role;
import com.example.back.entities.Utilisateur;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class JwtUtilTest {

    @Autowired JwtUtil jwtUtil;

    private Utilisateur utilisateur() {
        Utilisateur u = new Utilisateur();
        u.setId(1L); u.setPseudo("alice"); u.setEmail("alice@test.com");
        u.setMdp("hashed"); u.setDateCompte(LocalDate.now()); u.setRole(Role.USER);
        return u;
    }

    @Test @DisplayName("generateToken() génère un token valide")
    void generate_token_valide() {
        String token = jwtUtil.generateToken(utilisateur());
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // JWT = header.payload.signature
    }

    @Test @DisplayName("extractEmail() récupère l'email depuis le token")
    void extract_email() {
        String token = jwtUtil.generateToken(utilisateur());
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("alice@test.com");
    }

    @Test @DisplayName("validateToken() retourne true pour un token valide")
    void validate_token_valide() {
        String token = jwtUtil.generateToken(utilisateur());
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test @DisplayName("validateToken() retourne false pour un token falsifié")
    void validate_token_invalide() {
        assertThat(jwtUtil.validateToken("token.bidon.invalide")).isFalse();
    }

    @Test @DisplayName("generateRefreshToken() génère un refresh token différent de l'access token")
    void refresh_token_different_de_access_token() {
        String accessToken  = jwtUtil.generateToken(utilisateur());
        String refreshToken = jwtUtil.generateRefreshToken();
        assertThat(accessToken).isNotEqualTo(refreshToken);
    }

    @Test @DisplayName("validateRefreshToken() valide le refresh token généré")
    void validate_refresh_token() {
        String refreshToken = jwtUtil.generateRefreshToken();
        assertThat(jwtUtil.validateRefreshToken(refreshToken)).isTrue();
    }

    @Test @DisplayName("validateRefreshToken() rejette un token malformé")
    void validate_refresh_token_invalide() {
        assertThat(jwtUtil.validateRefreshToken("garbage")).isFalse();
    }
}