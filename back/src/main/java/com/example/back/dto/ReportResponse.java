package com.example.back.dto;

import com.example.back.entities.RaisonReport;
import com.example.back.entities.StatutReport;
import com.example.back.entities.TypeContenu;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportResponse {
    private Long id;
    private Long auteurId;
    private String auteurPseudo;
    private TypeContenu typeContenu;
    private Long idContenu;
    private RaisonReport raison;
    private String details;
    private LocalDateTime date;
    private StatutReport statut;
    private Long moderateurId;
    private String moderateurPseudo;
    private String noteModerateur;
}