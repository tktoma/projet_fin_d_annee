package com.example.back.controller;

import com.example.back.entities.Avis;
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
    public ResponseEntity<Avis> ajouterAvis(
            @PathVariable Long jeuId,
            @RequestBody String texte,
            Authentication auth) {
        Utilisateur u = (Utilisateur) auth.getPrincipal();
        return ResponseEntity.ok(
                avisService.ajouterAvis(u, jeuId, texte));
    }

    @PostMapping("/{avisId}/like")
    public ResponseEntity<Avis> liker(
            @PathVariable Long avisId,
            @RequestParam boolean like) {
        return ResponseEntity.ok(
                avisService.likerAvis(avisId, like));
    }

    @GetMapping("/jeu/{jeuId}")
    public ResponseEntity<List<Avis>> getAvisDuJeu(
            @PathVariable Long jeuId) {
        return ResponseEntity.ok(
                avisService.getAvisDuJeu(jeuId));
    }

    @DeleteMapping("/{avisId}")
    public ResponseEntity<Void> supprimer(
            @PathVariable Long avisId,
            Authentication auth) {
        Utilisateur u = (Utilisateur) auth.getPrincipal();
        avisService.supprimerAvis(u, avisId);
        return ResponseEntity.noContent().build();
    }
}
