package com.example.back.controller;

import com.example.back.dto.AvisDto;
import com.example.back.dto.AvisRequest;
import com.example.back.entities.Utilisateur;
import com.example.back.service.AvisService;
import jakarta.validation.Valid;
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
            @Valid @RequestBody AvisRequest request,
            Authentication auth) {
        Utilisateur u = (Utilisateur) auth.getPrincipal();
        return ResponseEntity.ok(
                avisService.ajouterAvis(u, jeuId, request.getTexte()));
    }

    @PostMapping("/{avisId}/like")
    public ResponseEntity<AvisDto> liker(
            @PathVariable Long avisId,
            @RequestParam boolean like,
            Authentication auth) {
        Utilisateur u = (Utilisateur) auth.getPrincipal();
        return ResponseEntity.ok(
                avisService.likerAvis(avisId, like, u));
    }

    @GetMapping("/jeu/{jeuId}")
    public ResponseEntity<List<AvisDto>> getAvisDuJeu(
            @PathVariable Long jeuId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                avisService.getAvisDuJeuPages(jeuId, page, size));
    }

    @GetMapping("/mes-avis")
    public ResponseEntity<List<AvisDto>> getMesAvis(
            Authentication auth) {
        Utilisateur u = (Utilisateur) auth.getPrincipal();
        return ResponseEntity.ok(
                avisService.getMesAvis(u.getId()));
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