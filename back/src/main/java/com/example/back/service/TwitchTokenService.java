package com.example.back.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.LocalDateTime;

@Service
public class TwitchTokenService {

    private String cachedToken;
    private LocalDateTime tokenExpiration;
    private final WebClient webClient;

    @Value("${igdb.client-id}")
    private String clientId;

    @Value("${igdb.client-secret}")
    private String clientSecret;

    public TwitchTokenService(@Qualifier("webClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @PostConstruct
    public void init() {
        // Récupération du token au démarrage
        refreshToken();
    }

    @Scheduled(fixedRate = 300000) // Toutes les 5 minutes
    public void checkAndRefreshToken() {
        if (shouldRefreshToken()) {
            refreshToken();
        }
    }

    public String getCurrentToken() {
        return cachedToken;
    }

    private boolean shouldRefreshToken() {
        return tokenExpiration == null ||
                tokenExpiration.minusMinutes(5).isBefore(LocalDateTime.now());
    }

    private void refreshToken() {
        try {
            TwitchTokenResponse response = webClient.post()
                    .uri("/oauth2/token")
                    .bodyValue("client_id=" + clientId + 
                             "&client_secret=" + clientSecret + 
                             "&grant_type=client_credentials")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .retrieve()
                    .bodyToMono(TwitchTokenResponse.class)
                    .block();

            if (response != null && response.getAccessToken() != null) {
                this.cachedToken = response.getAccessToken();
                this.tokenExpiration = LocalDateTime.now().plusSeconds(response.getExpiresIn());
                System.out.println("Token Twitch renouvelé avec succès, expiration: " + tokenExpiration);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du renouvellement du token Twitch: " + e.getMessage());
        }
    }

    // DTO pour la réponse de l'API Twitch
    private static class TwitchTokenResponse {
        @JsonProperty("access_token")
        private String access_token;

        @JsonProperty("expires_in")
        private int expires_in;

        private String token_type;

        public String getAccessToken() { return access_token; }
        public int getExpiresIn() { return expires_in; }
        public String getTokenType() { return token_type; }
    }
}
