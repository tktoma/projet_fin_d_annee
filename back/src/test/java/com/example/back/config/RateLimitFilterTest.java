package com.example.back.config;

import com.example.back.entities.Role;
import com.example.back.entities.Utilisateur;
import com.example.back.repository.UtilisateurRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @InjectMocks RateLimitFilter rateLimitFilter;

    private MockHttpServletRequest req(String path, String ip) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.setRequestURI(path);
        r.setRemoteAddr(ip);
        return r;
    }

    @Test @DisplayName("shouldNotFilter() retourne true pour les routes non protégées")
    void should_not_filter_autres_routes() {
        assertThat(rateLimitFilter.shouldNotFilter(req("/api/jeux", "1.2.3.4"))).isTrue();
        assertThat(rateLimitFilter.shouldNotFilter(req("/api/avis/jeu/1", "1.2.3.4"))).isTrue();
        assertThat(rateLimitFilter.shouldNotFilter(req("/swagger-ui.html", "1.2.3.4"))).isTrue();
    }

    @Test @DisplayName("shouldNotFilter() retourne false pour /api/auth/connexion")
    void should_filter_connexion() {
        assertThat(rateLimitFilter.shouldNotFilter(req("/api/auth/connexion", "1.2.3.4"))).isFalse();
    }

    @Test @DisplayName("shouldNotFilter() retourne false pour /api/auth/inscription")
    void should_filter_inscription() {
        assertThat(rateLimitFilter.shouldNotFilter(req("/api/auth/inscription", "1.2.3.4"))).isFalse();
    }

    @Test @DisplayName("10 requêtes passent, la 11e reçoit 429")
    void onzieme_requete_bloquee() throws Exception {
        MockHttpServletRequest request = req("/api/auth/connexion", "10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // 10 premières : OK
        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse resp = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, resp, chain);
            assertThat(resp.getStatus()).isNotEqualTo(429);
        }

        // 11e : bloquée
        rateLimitFilter.doFilterInternal(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("Trop de tentatives");
    }

    @Test @DisplayName("IPs différentes ont des compteurs indépendants")
    void ips_independantes() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        // IP A : 10 requêtes
        MockHttpServletRequest reqA = req("/api/auth/connexion", "192.168.1.1");
        for (int i = 0; i < 10; i++) {
            rateLimitFilter.doFilterInternal(reqA, new MockHttpServletResponse(), chain);
        }

        // IP B : première requête doit passer
        MockHttpServletRequest reqB = req("/api/auth/connexion", "192.168.1.2");
        MockHttpServletResponse respB = new MockHttpServletResponse();
        rateLimitFilter.doFilterInternal(reqB, respB, chain);
        assertThat(respB.getStatus()).isNotEqualTo(429);
    }

    @Test @DisplayName("X-Forwarded-For est utilisé si présent")
    void x_forwarded_for_prioritaire() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = req("/api/auth/connexion", "10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.42, 10.0.0.1");

        for (int i = 0; i < 10; i++) {
            rateLimitFilter.doFilterInternal(request, new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse resp11 = new MockHttpServletResponse();
        rateLimitFilter.doFilterInternal(request, resp11, chain);
        assertThat(resp11.getStatus()).isEqualTo(429);

        // La même IP réelle (10.0.0.1) sans header ne devrait pas encore être bloquée
        MockHttpServletRequest requestDirect = req("/api/auth/connexion", "10.0.0.1");
        MockHttpServletResponse respDirect = new MockHttpServletResponse();
        rateLimitFilter.doFilterInternal(requestDirect, respDirect, chain);
        assertThat(respDirect.getStatus()).isNotEqualTo(429);
    }

    @Test @DisplayName("la réponse 429 est en JSON avec le bon content-type")
    void reponse_429_est_json() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = req("/api/auth/connexion", "172.16.0.5");
        MockHttpServletResponse response = new MockHttpServletResponse();

        for (int i = 0; i < 11; i++) {
            response = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, response, chain);
        }
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("\"status\":429");
    }
}