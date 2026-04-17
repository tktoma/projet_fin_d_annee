package com.example.back.service;

import com.example.back.dto.BibliothequeDto;
import com.example.back.entities.Bibliotheque;
import com.example.back.entities.Jeu;
import com.example.back.entities.StatutJeu;
import com.example.back.entities.Utilisateur;
import com.example.back.repository.BibliothequeRepository;
import com.example.back.repository.JeuRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class BibliothequeService {

    private BibliothequeDto toDto(Bibliotheque b) {
        BibliothequeDto dto = new BibliothequeDto();
        dto.setId(b.getId());
        dto.setJeuId(b.getJeu().getId());
        dto.setJeuTitre(b.getJeu().getTitre());
        dto.setJeuCoverUrl(b.getJeu().getCoverUrl());
        dto.setStatut(b.getStatut());
        dto.setDate(b.getDate());
        return dto;
    }

    private final BibliothequeRepository bibliothequeRepository;
    private final JeuRepository jeuRepository;


    public BibliothequeService(
            BibliothequeRepository bibliothequeRepository,
            JeuRepository jeuRepository) {
        this.bibliothequeRepository = bibliothequeRepository;
        this.jeuRepository = jeuRepository;
    }

    // Ajouter un jeu à sa bibliothèque
    public BibliothequeDto ajouterJeu(Utilisateur utilisateur,
                                      Long jeuId,
                                      StatutJeu statut) {
        Jeu jeu = jeuRepository.findById(jeuId)
                .orElseThrow(() ->
                        new RuntimeException("Jeu introuvable"));

        Bibliotheque entree = bibliothequeRepository
                .findByUtilisateurIdAndJeuId(
                        utilisateur.getId(), jeuId)
                .orElse(new Bibliotheque());

        entree.setUtilisateur(utilisateur);
        entree.setJeu(jeu);
        entree.setStatut(statut);
        entree.setDate(LocalDate.now());
        Bibliotheque saved = bibliothequeRepository.save(entree);
        return toDto(saved);
    }

    // Changer le statut d'un jeu
    public BibliothequeDto changerStatut(Utilisateur utilisateur,
                                         Long jeuId,
                                         StatutJeu nouveauStatut) {
        Bibliotheque entree = bibliothequeRepository
                .findByUtilisateurIdAndJeuId(
                        utilisateur.getId(), jeuId)
                .orElseThrow(() ->
                        new RuntimeException("Jeu non trouvé " +
                                "dans la bibliothèque"));
        entree.setStatut(nouveauStatut);
        return toDto(bibliothequeRepository.save(entree));
    }


    // Toute la bibliothèque d'un user
    public List<BibliothequeDto> getBibliotheque(Long utilisateurId) {
        return bibliothequeRepository
                .findByUtilisateurId(utilisateurId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // Filtrer par statut
    public List<BibliothequeDto> getBibliothequeParStatut(
            Long utilisateurId, StatutJeu statut) {
        return bibliothequeRepository
                .findByUtilisateurIdAndStatut(utilisateurId, statut)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public void supprimerJeu(Utilisateur utilisateur,
                             Long jeuId) {
        Bibliotheque entree = bibliothequeRepository
                .findByUtilisateurIdAndJeuId(
                        utilisateur.getId(), jeuId)
                .orElseThrow(() ->
                        new RuntimeException("Entrée introuvable"));
        bibliothequeRepository.delete(entree);
    }
}