package com.example.back.controller;

import com.example.back.dto.IgdbGameDto;
import com.example.back.entities.Jeu;
import com.example.back.service.IgdbService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jeux")
public class JeuController {

    private final IgdbService igdbService;

    public JeuController(IgdbService igdbService) {
        this.igdbService = igdbService;
    }

    // POST — recherche sur IGDB (pas de sauvegarde)
    @PostMapping("/recherche")
    public ResponseEntity<List<IgdbGameDto>> rechercher(
            @RequestParam String titre) {
        return ResponseEntity.ok(
                igdbService.rechercherJeu(titre));
    }

    // POST — importe un jeu IGDB dans notre BDD
    @PostMapping("/importer/{igdbId}")
    public ResponseEntity<Jeu> importer(
            @PathVariable Long igdbId) {
        return ResponseEntity.ok(
                igdbService.importerJeu(igdbId));
    }

    // GET — liste tous les jeux de notre BDD
    @GetMapping
    public ResponseEntity<List<Jeu>> listerJeux() {
        return ResponseEntity.ok(
                igdbService.listerJeux());
    }
}
