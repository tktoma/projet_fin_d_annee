package com.example.back.controller;

import com.example.back.dto.NoteDto;
import com.example.back.entities.Utilisateur;
import com.example.back.service.NoteService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping("/jeu/{jeuId}")
    public ResponseEntity<NoteDto> noter(
            @PathVariable Long jeuId,
            @RequestParam
            @DecimalMin(value = "0.0", message = "La note doit être au minimum 0")
            @DecimalMax(value = "10.0", message = "La note doit être au maximum 10")
            Float valeur,
            Authentication auth) {
        Utilisateur u = (Utilisateur) auth.getPrincipal();
        return ResponseEntity.ok(
                noteService.noterJeu(u, jeuId, valeur));
    }

    @GetMapping("/jeu/{jeuId}")
    public ResponseEntity<List<NoteDto>> getNotesDuJeu(
            @PathVariable Long jeuId) {
        return ResponseEntity.ok(
                noteService.getNotesDuJeu(jeuId));
    }

    @GetMapping("/mes-notes")
    public ResponseEntity<List<NoteDto>> mesNotes(
            Authentication auth) {
        Utilisateur u = (Utilisateur) auth.getPrincipal();
        if (u == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return ResponseEntity.ok(
                noteService.getNotesUtilisateur(u.getId()));
    }

    @DeleteMapping("/jeu/{jeuId}")
    public ResponseEntity<Void> supprimer(
            @PathVariable Long jeuId,
            Authentication auth) {
        Utilisateur u = (Utilisateur) auth.getPrincipal();
        if (u == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        noteService.supprimerNote(u, jeuId);
        return ResponseEntity.noContent().build();
    }
}
