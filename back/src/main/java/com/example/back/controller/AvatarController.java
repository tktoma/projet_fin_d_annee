package com.example.back.controller;

import com.example.back.dto.AvatarDto;
import com.example.back.entities.Utilisateur;
import com.example.back.service.AvatarService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/utilisateurs")
public class AvatarController {

    private final AvatarService avatarService;

    public AvatarController(AvatarService avatarService) {
        this.avatarService = avatarService;
    }

    // Uploader ou remplacer son avatar
    @PostMapping("/moi/avatar")
    public ResponseEntity<AvatarDto> uploadAvatar(
            @RequestParam("fichier") MultipartFile fichier,
            Authentication auth) throws IOException {
        Utilisateur u = (Utilisateur) auth.getPrincipal();
        if (u == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return ResponseEntity.ok(
                avatarService.uploadAvatar(u, fichier));
    }

    // Voir l'avatar d'un utilisateur — public
    @GetMapping("/{id}/avatar")
    public ResponseEntity<AvatarDto> getAvatar(
            @PathVariable Long id) {
        return avatarService.getAvatar(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Supprimer son propre avatar
    @DeleteMapping("/moi/avatar")
    public ResponseEntity<Void> supprimerAvatar(
            Authentication auth) {
        Utilisateur u = (Utilisateur) auth.getPrincipal();
        if (u == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        avatarService.supprimerAvatar(u);
        return ResponseEntity.noContent().build();
    }
}