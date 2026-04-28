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
import org.springframework.web.cors.CorsConfigurationSource;

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
    private final RateLimitFilter rateLimitFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtFilter jwtFilter,
                          RateLimitFilter rateLimitFilter,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtFilter = jwtFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(CsrfConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(SWAGGER_WHITELIST).permitAll()

                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/**").permitAll()

                        .requestMatchers(HttpMethod.GET,
                                "/api/jeux").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/jeux/**").permitAll()

                        .requestMatchers(HttpMethod.GET,
                                "/api/avis/jeu/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/notes/jeu/**").permitAll()

                        .requestMatchers(HttpMethod.GET,
                                "/api/users/*/profil").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/users/*/avis").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/users/*/bibliotheque").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/utilisateurs/*/avatar").permitAll()

                        .requestMatchers("/avatars/**").permitAll()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(rateLimitFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}