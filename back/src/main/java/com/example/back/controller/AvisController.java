package com.example.back.controller;

import com.example.back.dto.AvisDto;
import com.example.back.entities.Utilisateur;
import com.example.back.service.AvisService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/avis")
public class AvisController {

    private final AvisService avisService;

    public AvisController(AvisService avisService) {
        this.avisService = avisService;
    }

    @PostMapping("/jeu/{jeuId}")
    public ResponseEntity<AvisDto> ajouterAvis(
            @PathVariable Long jeuId,
            @RequestBody
            @NotBlank(message = "Le texte ne peut pas être vide")
            @Size(min = 10, max = 2000,
                    message = "Le texte doit faire entre 10 et 2000 caractères")
            String texte,
            Authentication auth) {
        Utilisateur u = (Utilisateur) auth.getPrincipal();
        if (u == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return ResponseEntity.ok(
                avisService.ajouterAvis(u, jeuId, texte));
    }

    // Authentification requise — empêche le spam de likes anonymes
    @PostMapping("/{avisId}/like")
    public ResponseEntity<AvisDto> liker(
            @PathVariable Long avisId,
            @RequestParam boolean like,
            Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return ResponseEntity.ok(
                avisService.likerAvis(avisId, like));
    }

    // Paginé — évite les réponses trop lourdes sur les jeux populaires
    @GetMapping("/jeu/{jeuId}")
    public ResponseEntity<List<AvisDto>> getAvisDuJeu(
            @PathVariable Long jeuId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                avisService.getAvisDuJeuPages(jeuId, page, size));
    }

    @DeleteMapping("/{avisId}")
    public ResponseEntity<Void> supprimer(
            @PathVariable Long avisId,
            Authentication auth) {
        Utilisateur u = (Utilisateur) auth.getPrincipal();
        if (u == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        avisService.supprimerAvis(u, avisId);
        return ResponseEntity.noContent().build();
    }
}