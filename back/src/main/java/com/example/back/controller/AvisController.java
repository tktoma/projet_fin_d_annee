package com.example.back.controller;

import com.example.back.dto.AvisDto;
import com.example.back.entities.Utilisateur;
import com.example.back.service.AvisService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            @RequestBody String texte,
            Authentication auth) {
        Utilisateur u = (Utilisateur) auth.getPrincipal();
        if (u == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return ResponseEntity.ok(
                avisService.ajouterAvis(u, jeuId, texte));
    }

    @PostMapping("/{avisId}/like")
    public ResponseEntity<AvisDto> liker(
            @PathVariable Long avisId,
            @RequestParam boolean like) {
        return ResponseEntity.ok(
                avisService.likerAvis(avisId, like));
    }

    @GetMapping("/jeu/{jeuId}")
    public ResponseEntity<List<AvisDto>> getAvisDuJeu(
            @PathVariable Long jeuId) {
        return ResponseEntity.ok(
                avisService.getAvisDuJeu(jeuId));
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
