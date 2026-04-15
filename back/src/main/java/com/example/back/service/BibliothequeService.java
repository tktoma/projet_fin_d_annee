package com.example.back.service;

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

    private final BibliothequeRepository bibliothequeRepository;
    private final JeuRepository jeuRepository;

    public BibliothequeService(
            BibliothequeRepository bibliothequeRepository,
            JeuRepository jeuRepository) {
        this.bibliothequeRepository = bibliothequeRepository;
        this.jeuRepository = jeuRepository;
    }

    // Ajouter un jeu à sa bibliothèque
    public Bibliotheque ajouterJeu(Utilisateur utilisateur,
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
        return bibliothequeRepository.save(entree);
    }

    // Changer le statut d'un jeu
    public Bibliotheque changerStatut(Utilisateur utilisateur,
                                      Long jeuId,
                                      StatutJeu nouveauStatut) {
        Bibliotheque entree = bibliothequeRepository
                .findByUtilisateurIdAndJeuId(
                        utilisateur.getId(), jeuId)
                .orElseThrow(() ->
                        new RuntimeException("Jeu non trouvé " +
                                "dans la bibliothèque"));
        entree.setStatut(nouveauStatut);
        return bibliothequeRepository.save(entree);
    }

    // Toute la bibliothèque d'un user
    public List<Bibliotheque> getBibliotheque(
            Long utilisateurId) {
        return bibliothequeRepository
                .findByUtilisateurId(utilisateurId);
    }

    // Filtrer par statut
    public List<Bibliotheque> getBibliothequeParStatut(
            Long utilisateurId, StatutJeu statut) {
        return bibliothequeRepository
                .findByUtilisateurIdAndStatut(
                        utilisateurId, statut);
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