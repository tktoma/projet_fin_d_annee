package com.example.back.controller;

import com.example.back.dto.ReportRequest;
import com.example.back.dto.ReportResponse;
import com.example.back.dto.TraiterReportRequest;
import com.example.back.entities.StatutReport;
import com.example.back.entities.TypeContenu;
import com.example.back.entities.Utilisateur;
import com.example.back.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // Soumettre un report — tout utilisateur connecté
    @PostMapping
    public ResponseEntity<ReportResponse> soumettre(
            @Valid @RequestBody ReportRequest request,
            Authentication auth) {
        Utilisateur u = (Utilisateur) auth.getPrincipal();
        return ResponseEntity.ok(
                reportService.soumettre(u, request));
    }

    // Lister tous les reports — ADMIN + SUPERADMIN
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<ReportResponse>> listerTous() {
        return ResponseEntity.ok(reportService.listerTous());
    }

    // Filtrer par statut — ADMIN + SUPERADMIN
    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<ReportResponse>> parStatut(
            @PathVariable StatutReport statut) {
        return ResponseEntity.ok(
                reportService.listerParStatut(statut));
    }

    // Filtrer par type de contenu — ADMIN + SUPERADMIN
    @GetMapping("/type/{typeContenu}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<ReportResponse>> parType(
            @PathVariable TypeContenu typeContenu) {
        return ResponseEntity.ok(
                reportService.listerParType(typeContenu));
    }

    // Traiter un report — ADMIN + SUPERADMIN
    @PutMapping("/{id}/traiter")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<ReportResponse> traiter(
            @PathVariable Long id,
            @Valid @RequestBody TraiterReportRequest request,
            Authentication auth) {
        Utilisateur u = (Utilisateur) auth.getPrincipal();
        return ResponseEntity.ok(
                reportService.traiter(id, u, request));
    }

    // Supprimer un report — ADMIN + SUPERADMIN
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Void> supprimer(
            @PathVariable Long id) {
        reportService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}