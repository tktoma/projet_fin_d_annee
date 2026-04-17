package com.example.back.service;

import com.example.back.dto.AvisDto;
import com.example.back.entities.Avis;
import com.example.back.entities.Jeu;
import com.example.back.entities.Utilisateur;
import com.example.back.repository.AvisRepository;
import com.example.back.repository.JeuRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AvisService {

    private AvisDto toDto(Avis a) {
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

    private final AvisRepository avisRepository;
    private final JeuRepository jeuRepository;

    public AvisService(AvisRepository avisRepository,
                       JeuRepository jeuRepository) {
        this.avisRepository = avisRepository;
        this.jeuRepository = jeuRepository;
    }

    public AvisDto ajouterAvis(Utilisateur utilisateur,
                               Long jeuId, String texte) {
        Jeu jeu = jeuRepository.findById(jeuId)
                .orElseThrow(() ->
                        new RuntimeException("Jeu introuvable"));

        Avis avis = avisRepository
                .findByUtilisateurIdAndJeuId(
                        utilisateur.getId(), jeuId)
                .orElse(Avis.builder()   // ← builder au lieu de new Avis()
                .likes(0)        // ← valeurs explicites et claires
                .dislikes(0)
                .build());

        avis.setUtilisateur(utilisateur);
        avis.setJeu(jeu);
        avis.setTexte(texte);
        avis.setDate(LocalDate.now());
        return toDto(avisRepository.save(avis));
    }

    public AvisDto likerAvis(Long avisId, boolean like) {
        Avis avis = avisRepository.findById(avisId)
                .orElseThrow(() ->
                        new RuntimeException("Avis introuvable"));
        if (like) avis.setLikes(avis.getLikes() + 1);
        else avis.setDislikes(avis.getDislikes() + 1);
        return toDto(avisRepository.save(avis));
    }

    public List<AvisDto> getAvisDuJeu(Long jeuId) {
        return avisRepository.findByJeuId(jeuId)
                .stream().map(this::toDto).toList();
    }

    public void supprimerAvis(Utilisateur utilisateur,
                              Long avisId) {
        Avis avis = avisRepository.findById(avisId)
                .orElseThrow(() ->
                        new RuntimeException("Avis introuvable"));
        if (!avis.getUtilisateur().getId().equals(utilisateur.getId())) {
            throw new RuntimeException("Non autorisé");
        }
        avisRepository.delete(avis);
    }
}
