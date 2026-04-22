package com.example.back.controller;

import com.example.back.dto.BibliothequeDto;
import com.example.back.entities.StatutJeu;
import com.example.back.entities.Utilisateur;
import com.example.back.service.BibliothequeService;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bibliotheque")
public class BibliothequeController {

    private final BibliothequeService bibliothequeService;

    public BibliothequeController(
            BibliothequeService bibliothequeService) {
        this.bibliothequeService = bibliothequeService;
    }

    @PostMapping("/jeu/{jeuId}")
    public ResponseEntity<BibliothequeDto> ajouterJeu(
            @PathVariable Long jeuId,
            @RequestParam StatutJeu statut,
            Authentication auth) {
        Utilisateur u = (Utilisateur) auth.getPrincipal();
        if (u == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return ResponseEntity.ok(
                bibliothequeService.ajouterJeu(u, jeuId, statut));
    }

    @PutMapping("/jeu/{jeuId}/statut")
    public ResponseEntity<BibliothequeDto> changerStatut(
            @PathVariable Long jeuId,
            @RequestParam StatutJeu statut,
            Authentication auth) {
        Utilisateur u = (Utilisateur) auth.getPrincipal();
        if (u == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return ResponseEntity.ok(
                bibliothequeService.changerStatut(u, jeuId, statut));
    }

    @GetMapping
    public ResponseEntity<List<BibliothequeDto>> maBibliotheque(
            Authentication auth) {
        Utilisateur u = (Utilisateur) auth.getPrincipal();
        if (u == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return ResponseEntity.ok(
                bibliothequeService.getBibliotheque(u.getId()));
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<BibliothequeDto>> parStatut(
            @PathVariable StatutJeu statut,
            Authentication auth) {
        Utilisateur u = (Utilisateur) auth.getPrincipal();
        if (u == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return ResponseEntity.ok(
                bibliothequeService.getBibliothequeParStatut(
                        u.getId(), statut)); // ← utiliser le paramètre statut ici
    }


    @DeleteMapping("/jeu/{jeuId}")
    public ResponseEntity<Void> supprimer(
            @PathVariable Long jeuId,
            Authentication auth) {
        Utilisateur u = (Utilisateur) auth.getPrincipal();
        if (u == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        bibliothequeService.supprimerJeu(u, jeuId);
        return ResponseEntity.noContent().build();
    }
}