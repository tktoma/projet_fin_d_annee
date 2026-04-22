package com.example.back.controller;

import com.example.back.dto.IgdbGameDto;
import com.example.back.dto.ImportResult;
import com.example.back.dto.JeuResponse;
import com.example.back.dto.PageResponse;
import com.example.back.service.IgdbService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jeux")
public class JeuController {

    private final IgdbService igdbService;

    public JeuController(IgdbService igdbService) {
        this.igdbService = igdbService;
    }

    /**
     * Liste paginée avec filtres combinés, tous optionnels.
     *
     * GET /api/jeux                              → tous les jeux
     * GET /api/jeux?genre=RPG                    → filtrer par genre
     * GET /api/jeux?plateforme=PC                → filtrer par plateforme
     * GET /api/jeux?noteMin=7.5                  → note moyenne >= 7.5
     * GET /api/jeux?titre=zelda&genre=RPG&noteMin=8  → combiné
     * GET /api/jeux?page=1&size=10&sort=noteMoyenne  → pagination et tri
     */
    @GetMapping
    public ResponseEntity<PageResponse<JeuResponse>> listerJeux(
            @RequestParam(required = false) String titre,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String plateforme,
            @RequestParam(required = false) Float noteMin,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size,
            @RequestParam(defaultValue = "titre") String sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<JeuResponse> pageResult = igdbService
                .rechercherAvecFiltres(titre, genre, plateforme,
                        noteMin, pageable);

        PageResponse<JeuResponse> response = new PageResponse<>();
        response.setContent(pageResult.getContent());
        response.setPage(pageResult.getNumber());
        response.setSize(pageResult.getSize());
        response.setTotalElements(pageResult.getTotalElements());
        response.setTotalPages(pageResult.getTotalPages());
        response.setFirst(pageResult.isFirst());
        response.setLast(pageResult.isLast());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/importer/{igdbId}")
    @PreAuthorize("hasAnyRole('POSTER','ADMIN','SUPERADMIN')")
    public ResponseEntity<JeuResponse> importer(
            @PathVariable Long igdbId) {
        return ResponseEntity.ok(igdbService.importerJeu(igdbId));
    }

    @PostMapping("/recherche")
    public ResponseEntity<List<IgdbGameDto>> rechercher(
            @RequestParam String titre) {
        return ResponseEntity.ok(igdbService.rechercherJeu(titre));
    }

    @PostMapping("/importer-tous")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<String> importerTousLesJeux(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "500") int limit) {

        ImportResult result = igdbService.importerJeuxPagines(page, limit);
        return ResponseEntity.ok(
                "Import page " + page + " : " + result.getImportes()
                        + " jeux, total : " + result.getTotal());
    }
}
