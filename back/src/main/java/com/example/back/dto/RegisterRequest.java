package com.example.back.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String pseudo;
    private String email;
    private String motDePasse;
}