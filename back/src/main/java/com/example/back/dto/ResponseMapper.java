package com.example.back.dto;

import com.example.back.entities.*;

public class ResponseMapper {

    private ResponseMapper() {}

    public static UtilisateurResponse toUtilisateurResponse(
            Utilisateur u) {
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

    public static AvatarDto toAvatarDto(Avatar a) {
        AvatarDto dto = new AvatarDto();
        dto.setId(a.getId());
        dto.setUtilisateurId(a.getUtilisateur().getId());
        dto.setUrl(a.getUrl());
        dto.setContentType(a.getContentType());
        dto.setTaille(a.getTaille());
        return dto;
    }

    public static ReportResponse toReportResponse(Report r) {
        ReportResponse dto = new ReportResponse();
        dto.setId(r.getId());
        dto.setAuteurId(r.getAuteur().getId());
        dto.setAuteurPseudo(r.getAuteur().getPseudo());
        dto.setTypeContenu(r.getTypeContenu());
        dto.setIdContenu(r.getIdContenu());
        dto.setRaison(r.getRaison());
        dto.setDetails(r.getDetails());
        dto.setDate(r.getDate());
        dto.setStatut(r.getStatut());
        if (r.getModerateur() != null) {
            dto.setModerateurId(r.getModerateur().getId());
            dto.setModerateurPseudo(r.getModerateur().getPseudo());
        }
        dto.setNoteModerateur(r.getNoteModerateur());
        return dto;
    }
    public static AvisDto toAvisDto(Avis a) {
        AvisDto dto = new AvisDto();
        dto.setId(a.getId());
        dto.setJeuId(a.getJeu().getId());
        dto.setJeuTitre(a.getJeu().getTitre());
        dto.setUtilisateurId(a.getUtilisateur().getId());
        dto.setUtilisateurPseudo(a.getUtilisateur().getPseudo());
        dto.setTexte(a.getTexte());
        dto.setLikes(a.getLikes());
        dto.setDislikes(a.getDislikes());
        dto.setDate(a.getDate());
        return dto;
    }

    public static NoteDto toNoteDto(Note n) {
        NoteDto dto = new NoteDto();
        dto.setId(n.getId());
        dto.setJeuId(n.getJeu().getId());
        dto.setJeuTitre(n.getJeu().getTitre());
        dto.setUtilisateurId(n.getUtilisateur().getId());
        dto.setUtilisateurPseudo(n.getUtilisateur().getPseudo());
        dto.setValeur(n.getValeur());
        dto.setDate(n.getDate());
        return dto;
    }

    public static BibliothequeDto toBibliothequeDto(Bibliotheque b) {
        BibliothequeDto dto = new BibliothequeDto();
        dto.setId(b.getId());
        dto.setJeuId(b.getJeu().getId());
        dto.setJeuTitre(b.getJeu().getTitre());
        dto.setJeuCoverUrl(b.getJeu().getCoverUrl());
        dto.setStatut(b.getStatut());
        dto.setDate(b.getDate());
        return dto;
    }
}