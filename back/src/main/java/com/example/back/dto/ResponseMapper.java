package com.example.back.dto;

import com.example.back.entities.Jeu;
import com.example.back.entities.Utilisateur;

public class ResponseMapper {

    private ResponseMapper() {}

    public static UtilisateurResponse toUtilisateurResponse(Utilisateur u) {
        UtilisateurResponse dto = new UtilisateurResponse();
        dto.setId(u.getId());
        dto.setPseudo(u.getPseudo());
        dto.setEmail(u.getEmail());
        dto.setRole(u.getRole());
        dto.setDateCompte(u.getDateCompte());
        return dto;
    }

    public static JeuResponse toJeuResponse(Jeu j) {
        JeuResponse dto = new JeuResponse();
        dto.setId(j.getId());
        dto.setTitre(j.getTitre());
        dto.setGenre(j.getGenre());
        dto.setPlateforme(j.getPlateforme());
        dto.setCoverUrl(j.getCoverUrl());
        dto.setDateSortie(j.getDateSortie());
        dto.setNoteMoyenne(j.getNoteMoyenne());
        dto.setSource(j.getSource());
        dto.setExternalId(j.getExternalId());
        return dto;
    }
}
