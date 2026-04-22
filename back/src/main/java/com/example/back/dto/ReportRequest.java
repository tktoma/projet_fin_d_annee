package com.example.back.dto;

import com.example.back.entities.RaisonReport;
import com.example.back.entities.TypeContenu;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportRequest {

    @NotNull(message = "Le type de contenu est obligatoire")
    private TypeContenu typeContenu;

    @NotNull(message = "L'id du contenu est obligatoire")
    private Long idContenu;

    @NotNull(message = "La raison est obligatoire")
    private RaisonReport raison;

    private String details;
}
