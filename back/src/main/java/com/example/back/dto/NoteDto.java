package com.example.back.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class NoteDto {
    private Long id;
    private Long jeuId;
    private String jeuTitre;
    private Long utilisateurId;
    private String utilisateurPseudo;
    private float valeur;
    private LocalDate date;
}
