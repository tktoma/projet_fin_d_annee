package com.example.back.dto;

import com.example.back.entities.StatutJeu;
import lombok.Data;
import java.time.LocalDate;

@Data
public class BibliothequeDto {
    private Long id;
    private Long jeuId;
    private String jeuTitre;
    private String jeuCoverUrl;
    private StatutJeu statut;
    private LocalDate date;
}
