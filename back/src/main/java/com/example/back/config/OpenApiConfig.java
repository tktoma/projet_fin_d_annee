package com.example.back.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Swagger/OpenAPI — désactivé en profil prod.
 * La whitelist dans SecurityConfig reste en place mais le bean
 * n'est pas créé, donc /swagger-ui.html retourne 404 en prod.
 */
@Configuration
@Profile("!prod")
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GameLib API")
                        .description("API de gestion de bibliothèque de jeux vidéo")
                        .version("1.0.0"))
                .addSecurityItem(
                        new SecurityRequirement().addList(SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(
                                                "Coller le token JWT obtenu via POST /api/auth/connexion")));
    }
}