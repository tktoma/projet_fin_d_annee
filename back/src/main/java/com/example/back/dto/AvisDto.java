package com.example.back.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AvisDto {
    private Long id;
    private Long jeuId;
    private String jeuTitre;
    private Long utilisateurId;
    private String utilisateurPseudo;
    private String texte;
    private Integer likes;
    private Integer dislikes;
    private LocalDate date;
}