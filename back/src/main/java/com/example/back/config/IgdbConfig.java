package com.example.back.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Configuration
public class IgdbConfig {

    @Value("${igdb.client-id}")
    private String clientId;

    @Value("${igdb.client-secret}")
    private String clientSecret;

    @Bean
    public WebClient igdbWebClient() {
        // 1. On récupère le token Twitch
        String token = fetchTwitchToken();

        // 2. On configure le WebClient avec les headers IGDB
        return WebClient.builder()
                .baseUrl("https://api.igdb.com/v4")
                .defaultHeader("Client-ID", clientId)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }

    private String fetchTwitchToken() {
        String url = "https://id.twitch.tv/oauth2/token"
                + "?client_id=" + clientId
                + "&client_secret=" + clientSecret
                + "&grant_type=client_credentials";

        Map response = WebClient.create()
                .post()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return (String) response.get("access_token");
    }
}
