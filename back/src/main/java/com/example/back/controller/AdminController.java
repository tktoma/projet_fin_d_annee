package com.example.back.controller;

import com.example.back.dto.UtilisateurResponse;
import com.example.back.entities.Role;
import com.example.back.entities.Utilisateur;
import com.example.back.service.AdminService;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/utilisateurs")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<UtilisateurResponse>> listerUsers() {
        return ResponseEntity.ok(
                adminService.listerUtilisateurs());
    }

    @PutMapping("/utilisateurs/{id}/role")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<UtilisateurResponse> changerRole(
            @PathVariable Long id,
            @RequestParam Role role,
            Authentication auth) {
        Utilisateur demandeur = (Utilisateur) auth.getPrincipal();
        return ResponseEntity.ok(
                adminService.changerRole(id, role, demandeur));
    }

    @DeleteMapping("/utilisateurs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Void> supprimerUser(
            @PathVariable Long id,
            Authentication auth) {
        Utilisateur demandeur = (Utilisateur) auth.getPrincipal();
        adminService.supprimerUtilisateur(id, demandeur);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/avis/{avisId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Void> supprimerAvis(
            @PathVariable Long avisId) {
        adminService.supprimerAvis(avisId);
        return ResponseEntity.noContent().build();
    }
}