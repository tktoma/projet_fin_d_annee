package com.example.back.config;

import com.example.back.service.TwitchTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Configuration
public class IgdbConfig {

    @Value("${igdb.client-id}")
    private String clientId;

    @Value("${igdb.client-secret}")
    private String clientSecret;

    @Bean
    public WebClient igdbWebClient(TwitchTokenService twitchTokenService) {
        return WebClient.builder()
                .baseUrl("https://api.igdb.com/v4")
                .defaultHeader("Client-ID", clientId)
                .filter((request, next) -> {
                    // ← token récupéré dynamiquement à CHAQUE requête
                    ClientRequest newRequest = ClientRequest.from(request)
                            .header("Authorization",
                                    "Bearer " + twitchTokenService.getCurrentToken())
                            .build();
                    return next.exchange(newRequest);
                })
                .build();
    }

}
