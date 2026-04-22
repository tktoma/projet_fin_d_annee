package com.example.back.dto;

import com.example.back.entities.StatutReport;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TraiterReportRequest {

    @NotNull(message = "Le statut est obligatoire")
    private StatutReport statut;

    private String note;
}