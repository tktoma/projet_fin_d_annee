package com.example.back.dto;

import com.example.back.entities.Role;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ProfilResponse {
    private Long id;
    private String pseudo;
    private Role role;
    private LocalDate dateCompte;
    private int nombreJeux;
    private int nombreAvis;
    private int nombreNotes;
    private List<AvisDto> derniersAvis;
    private List<NoteDto> dernieresNotes;
}