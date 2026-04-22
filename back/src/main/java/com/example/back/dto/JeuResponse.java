package com.example.back.dto;

import lombok.Data;

import java.time.LocalDate;

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
}
