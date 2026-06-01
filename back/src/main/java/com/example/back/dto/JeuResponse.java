package com.example.back.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
public class JeuResponse {
    private Long id;
    private String titre;
    private String genre;
    private String plateforme;
    private String coverUrl;
    private LocalDate dateSortie;
    private float noteMoyenne;
    private String source;
    private String externalId;
    private String description;
    private long vues;

    // Stats bibliothèque — remplis uniquement sur la fiche détail
    private long nbBibliotheque;               // total personnes qui ont ce jeu
    private Map<String, Long> statutStats;     // ex: {JOUER:12, FINIT:34, ...}
}