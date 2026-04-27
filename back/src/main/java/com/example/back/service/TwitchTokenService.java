package com.example.back.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;

@Service
public class TwitchTokenService {

    private static final Logger log =
            LoggerFactory.getLogger(TwitchTokenService.class);

    private volatile String cachedToken;
    private volatile LocalDateTime tokenExpiration;

    private final WebClient webClient;

    @Value("${igdb.client-id}")
    private String clientId;

    @Value("${igdb.client-secret}")
    private String clientSecret;

    public TwitchTokenService(
            @Qualifier("webClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @PostConstruct
    public void init() {
        try {
            refreshToken();
        } catch (Exception e) {
            log.warn("Impossible d'obtenir le token Twitch au démarrage "
                            + "— les appels IGDB échoueront jusqu'au prochain refresh : {}",
                    e.getMessage());
        }
    }

    @Scheduled(fixedRate = 300_000)
    public void checkAndRefreshToken() {
        if (shouldRefreshToken()) {
            try {
                refreshToken();
            } catch (Exception e) {
                log.error("Échec du refresh token Twitch planifié : {}",
                        e.getMessage());
            }
        }
    }

    public String getCurrentToken() {
        if (cachedToken == null) {
            throw new IllegalStateException(
                    "Token Twitch non disponible — vérifiez IGDB_CLIENT_ID "
                            + "et IGDB_CLIENT_SECRET");
        }
        return cachedToken;
    }

    private boolean shouldRefreshToken() {
        return tokenExpiration == null
                || tokenExpiration.minusMinutes(5)
                .isBefore(LocalDateTime.now());
    }

    private void refreshToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("grant_type", "client_credentials");

        TwitchTokenResponse response = webClient.post()
                .uri("/oauth2/token")
                .bodyValue(form)
                .header("Content-Type",
                        "application/x-www-form-urlencoded")
                .retrieve()
                .bodyToMono(TwitchTokenResponse.class)
                .block();

        if (response != null
                && response.getAccessToken() != null) {
            this.cachedToken = response.getAccessToken();
            this.tokenExpiration = LocalDateTime.now()
                    .plusSeconds(response.getExpiresIn());
            log.info("Token Twitch renouvelé, expiration : {}",
                    tokenExpiration);
        } else {
            log.error("Réponse Twitch invalide ou token absent");
        }
    }

    @Data
    private static class TwitchTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("expires_in")
        private int expiresIn;
    }
}