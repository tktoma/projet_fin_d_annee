package com.example.back.controller;

import com.example.back.dto.IgdbGameDto;
import com.example.back.dto.ImportProgress;
import com.example.back.dto.ImportResult;
import com.example.back.dto.JeuManuelRequest;
import com.example.back.dto.JeuResponse;
import com.example.back.dto.PageResponse;
import com.example.back.service.IgdbService;
import jakarta.validation.Valid;
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

    @GetMapping
    public ResponseEntity<PageResponse<JeuResponse>> listerJeux(
            @RequestParam(required = false) String titre,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String plateforme,
            @RequestParam(required = false) Float  noteMin,
            @RequestParam(required = false) Integer anneeMin,
            @RequestParam(required = false) Integer anneeMax,
            @RequestParam(defaultValue = "0")      int page,
            @RequestParam(defaultValue = "20")     int size,
            @RequestParam(defaultValue = "titre")  String sort) {

        // Pour popularite/vues/noteMoyenne/dateSortie le tri est géré dans le service
        Pageable pageable = PageRequest.of(page, size, Sort.by("titre"));

        Page<JeuResponse> pageResult = igdbService.rechercherAvecFiltres(
                titre, genre, plateforme, noteMin, anneeMin, anneeMax, sort, pageable);

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

    @GetMapping("/genres")
    public ResponseEntity<List<String>> getGenres() {
        return ResponseEntity.ok(igdbService.getGenres());
    }

    @GetMapping("/plateformes")
    public ResponseEntity<List<String>> getPlateformes() {
        return ResponseEntity.ok(igdbService.getPlateformes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JeuResponse> getJeuById(@PathVariable Long id) {
        return ResponseEntity.ok(igdbService.getJeuById(id));
    }

    @PostMapping("/importer/{igdbId}")
    @PreAuthorize("hasAnyRole('POSTER','ADMIN','SUPERADMIN')")
    public ResponseEntity<JeuResponse> importer(@PathVariable Long igdbId) {
        return ResponseEntity.ok(igdbService.importerJeu(igdbId));
    }

    @PostMapping("/recherche")
    public ResponseEntity<List<IgdbGameDto>> rechercher(@RequestParam String titre) {
        return ResponseEntity.ok(igdbService.rechercherJeu(titre));
    }

    @PostMapping("/manuel")
    @PreAuthorize("hasAnyRole('POSTER','ADMIN','SUPERADMIN')")
    public ResponseEntity<JeuResponse> creerManuellement(
            @Valid @RequestBody JeuManuelRequest request) {
        return ResponseEntity.ok(igdbService.creerJeuManuellement(request));
    }

    @PostMapping("/import-auto")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<ImportProgress> importerAuto() {
        igdbService.lancerImportComplet();
        return ResponseEntity.ok(igdbService.getProgress());
    }

    @GetMapping("/import-progression")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<ImportProgress> getProgression() {
        return ResponseEntity.ok(igdbService.getProgress());
    }

    @PostMapping("/importer-tous")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<String> importerTousLesJeux(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "500") int limit) {
        ImportResult result = igdbService.importerJeuxPagines(page, limit);
        return ResponseEntity.ok("Import page " + page + " : "
                + result.getImportes() + " jeux, total : " + result.getTotal());
    }

    @DeleteMapping("/cache")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Void> viderCache() {
        igdbService.viderCacheRecherches();
        return ResponseEntity.noContent().build();
    }
}