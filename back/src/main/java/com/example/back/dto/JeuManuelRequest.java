package com.example.back.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JeuManuelRequest {

    @NotBlank(message = "Le titre est obligatoire")
    private String titre;

    private String genre;
    private String plateforme;
    private String coverUrl;
    private String dateSortie; // format ISO : "2023-09-22"
    private String description;
}