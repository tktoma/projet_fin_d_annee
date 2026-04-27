package com.example.back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AvisRequest {

    @NotBlank(message = "Le texte ne peut pas être vide")
    @Size(min = 10, max = 2000,
            message = "Le texte doit faire entre 10 et 2000 caractères")
    private String texte;
}