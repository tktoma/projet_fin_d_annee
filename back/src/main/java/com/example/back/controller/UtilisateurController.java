package com.example.back.controller;

import com.example.back.dto.AvisDto;
import com.example.back.dto.BibliothequeDto;
import com.example.back.dto.ProfilResponse;
import com.example.back.service.UtilisateurService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    /**
     * Profil public complet : pseudo, rôle, date d'inscription,
     * compteurs et les 5 derniers avis/notes.
     * Accessible sans authentification.
     */
    @GetMapping("/{id}/profil")
    public ResponseEntity<ProfilResponse> getProfil(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                utilisateurService.getProfilPublic(id));
    }

    /**
     * Tous les avis d'un utilisateur, du plus récent au plus ancien.
     * Accessible sans authentification.
     */
    @GetMapping("/{id}/avis")
    public ResponseEntity<List<AvisDto>> getAvis(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                utilisateurService.getAvisPublics(id));
    }

    /**
     * Bibliothèque complète d'un utilisateur avec statuts.
     * Accessible sans authentification.
     */
    @GetMapping("/{id}/bibliotheque")
    public ResponseEntity<List<BibliothequeDto>> getBibliotheque(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                utilisateurService.getBibliothequePublique(id));
    }
}