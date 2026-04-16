package com.example.back.controller;

import com.example.back.dto.IgdbGameDto;
import com.example.back.dto.ImportResult;
import com.example.back.dto.PageResponse;
import com.example.back.entities.Jeu;
import com.example.back.service.IgdbService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.example.back.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jeux")
public class JeuController {

    private final IgdbService igdbService;

    private PageResponse<Jeu> convertToPageResponse(Page<Jeu> page) {
        PageResponse<Jeu> response = new PageResponse<>();
        response.setContent(page.getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setFirst(page.isFirst());
        response.setLast(page.isLast());
        return response;
    }

    public JeuController(IgdbService igdbService) {
        this.igdbService = igdbService;
    }



    // Seuls POSTER, ADMIN, SUPERADMIN peuvent importer un jeu
    @PostMapping("/importer/{igdbId}")
    @PreAuthorize("hasAnyRole('POSTER','ADMIN','SUPERADMIN')")
    public ResponseEntity<Jeu> importer(
            @PathVariable Long igdbId) {
        return ResponseEntity.ok(
                igdbService.importerJeu(igdbId));
    }

    // Recherche — publique, pas de @PreAuthorize
    @PostMapping("/recherche")
    public ResponseEntity<List<IgdbGameDto>> rechercher(
            @RequestParam String titre) {
        return ResponseEntity.ok(
                igdbService.rechercherJeu(titre));
    }


    // GET — liste tous les jeux de notre BDD avec pagination
    @GetMapping
    public ResponseEntity<PageResponse<Jeu>> listerJeux(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "titre") String sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<Jeu> pageResult = igdbService.listerJeux(pageable);

        return ResponseEntity.ok(convertToPageResponse(pageResult));
    }
    // Importer tous les jeux de l'API IGDB
    @PostMapping("/importer-tous")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<String> importerTousLesJeux(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "500") int limit) {

        ImportResult result = igdbService.importerJeuxPagines(page, limit);
        return ResponseEntity.ok(
                "Import page " + page + " : " + result.getImportes() +
                        " jeux, total : " + result.getTotal());
    }

}

