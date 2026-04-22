package com.example.back.dto;

import lombok.Data;

@Data
public class AvatarDto {
    private Long id;
    private Long utilisateurId;
    private String url;
    private String contentType;
    private Long taille;
}