package com.example.back.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] SWAGGER_WHITELIST = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml"
    };

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                .csrf(CsrfConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // Swagger
                        .requestMatchers(SWAGGER_WHITELIST).permitAll()

                        // Auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/**").permitAll()

                        // Jeux
                        .requestMatchers(HttpMethod.POST,
                                "/api/jeux/recherche").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/jeux").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/jeux/**").permitAll()

                        // Avis et notes publics
                        .requestMatchers(HttpMethod.GET,
                                "/api/avis/jeu/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/notes/jeu/**").permitAll()

                        // Profils et avatars publics
                        .requestMatchers(HttpMethod.GET,
                                "/api/users/*/profil").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/users/*/avis").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/users/*/bibliotheque").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/utilisateurs/*/avatar").permitAll()

                        // Fichiers avatar servis statiquement
                        .requestMatchers("/avatars/**").permitAll()

                        // Tout le reste → token requis
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}