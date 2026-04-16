package com.example.back.service;

import com.example.back.config.JwtUtil;
import com.example.back.dto.AuthResponse;
import com.example.back.dto.LoginRequest;
import com.example.back.dto.RegisterRequest;
import com.example.back.entities.Utilisateur;
import com.example.back.repository.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UtilisateurService(
            UtilisateurRepository utilisateurRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // Inscription
    public AuthResponse inscrire(RegisterRequest request) {
        if (utilisateurRepository.existsByEmail(
                request.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }
        if (utilisateurRepository.existsByPseudo(
                request.getPseudo())) {
            throw new RuntimeException("Pseudo déjà utilisé");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setPseudo(request.getPseudo());
        utilisateur.setEmail(request.getEmail());
        utilisateur.setMdp(
                passwordEncoder.encode(request.getMotDePasse()));
        utilisateur.setDateCompte(LocalDate.now());

        utilisateurRepository.save(utilisateur);

        String token = jwtUtil.generateToken(utilisateur);
        String refreshToken = jwtUtil.generateRefreshToken();
        utilisateur.setRefreshToken(refreshToken);
        utilisateur.setRefreshTokenExpiration(LocalDateTime.now().plusDays(30));
        utilisateurRepository.save(utilisateur);

        return new AuthResponse(
                token,
                refreshToken,
                utilisateur.getPseudo(),
                utilisateur.getId());


    }

    // Connexion
    public AuthResponse connecter(LoginRequest request) {
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Email introuvable"));

        if (!passwordEncoder.matches(
                request.getMotDePasse(),
                utilisateur.getMdp())) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        String token = jwtUtil.generateToken(utilisateur);
        String refreshToken = jwtUtil.generateRefreshToken();
        utilisateur.setRefreshToken(refreshToken);
        utilisateur.setRefreshTokenExpiration(LocalDateTime.now().plusDays(30));
        utilisateurRepository.save(utilisateur);

        return new AuthResponse(
                token,
                refreshToken,
                utilisateur.getPseudo(),
                utilisateur.getId());
    }
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new RuntimeException("Refresh token invalide");
        }

        Utilisateur utilisateur = utilisateurRepository
                .findByRefreshToken(refreshToken)
                .orElseThrow(() ->
                        new RuntimeException("Refresh token introuvable"));

        if (utilisateur.getRefreshTokenExpiration().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expiré");
        }

        String newToken = jwtUtil.generateToken(utilisateur);
        String newRefreshToken = jwtUtil.generateRefreshToken();
        utilisateur.setRefreshToken(newRefreshToken);
        utilisateur.setRefreshTokenExpiration(LocalDateTime.now().plusDays(30));
        utilisateurRepository.save(utilisateur);

        return new AuthResponse(
                newToken,
                newRefreshToken,
                utilisateur.getPseudo(),
                utilisateur.getId());
    }
}