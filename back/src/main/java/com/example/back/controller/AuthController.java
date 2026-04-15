package com.example.back.controller;

import com.example.back.dto.AuthResponse;
import com.example.back.dto.LoginRequest;
import com.example.back.dto.RegisterRequest;
import com.example.back.service.UtilisateurService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UtilisateurService utilisateurService;

    public AuthController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @PostMapping("/inscription")
    public ResponseEntity<AuthResponse> inscrire(
            @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(
                utilisateurService.inscrire(request));
    }

    @PostMapping("/connexion")
    public ResponseEntity<AuthResponse> connecter(
            @RequestBody LoginRequest request) {
        return ResponseEntity.ok(
                utilisateurService.connecter(request));
    }
}