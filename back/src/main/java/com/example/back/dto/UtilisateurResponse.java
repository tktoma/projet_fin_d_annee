package com.example.back.dto;

import com.example.back.entities.Role;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UtilisateurResponse {
    private Long id;
    private String pseudo;
    private String email;
    private Role role;
    private LocalDate dateCompte;
}