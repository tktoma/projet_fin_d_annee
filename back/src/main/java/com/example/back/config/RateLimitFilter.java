package com.example.back.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting simple sur les endpoints d'authentification.
 * Limite à 10 tentatives par IP sur une fenêtre glissante de 15 minutes.
 * La tâche @Scheduled purge les entrées expirées toutes les 30 minutes
 * pour éviter une fuite mémoire sur les IPs distinctes.
 * Pour la production multi-instances, préférer Bucket4j + Redis.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS = 15 * 60 * 1000L; // 15 minutes

    // ip -> [compteur, timestamp début de fenêtre]
    private final Map<String, long[]> attempts = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.equals("/api/auth/connexion")
                && !path.equals("/api/auth/inscription");
    }

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain)
            throws ServletException, IOException {

        String ip = getClientIp(request);
        long now = Instant.now().toEpochMilli();

        attempts.compute(ip, (key, val) -> {
            if (val == null || now - val[1] > WINDOW_MS) {
                return new long[]{1, now};
            }
            val[0]++;
            return val;
        });

        long[] state = attempts.get(ip);
        if (state[0] > MAX_ATTEMPTS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"message\":\"Trop de tentatives. Réessayez dans 15 minutes.\","
                            + "\"status\":429}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Purge les entrées dont la fenêtre de 15 minutes est expirée.
     * Tourne toutes les 30 minutes — empêche la croissance illimitée
     * de la map en cas de nombreuses IPs distinctes.
     */
    @Scheduled(fixedRate = 30 * 60 * 1000L)
    public void purgerEntreesExpirees() {
        long now = Instant.now().toEpochMilli();
        attempts.entrySet().removeIf(entry ->
                now - entry.getValue()[1] > WINDOW_MS);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}