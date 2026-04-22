package com.example.back.service;

import com.example.back.dto.BibliothequeDto;
import com.example.back.dto.ResponseMapper;
import com.example.back.entities.Bibliotheque;
import com.example.back.entities.Jeu;
import com.example.back.entities.StatutJeu;
import com.example.back.entities.Utilisateur;
import com.example.back.exception.NotFoundException;
import com.example.back.repository.BibliothequeRepository;
import com.example.back.repository.JeuRepository;
import jakarta.transaction.Transactional;
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
    @Transactional
    public BibliothequeDto ajouterJeu(Utilisateur utilisateur,
                                      Long jeuId,
                                      StatutJeu statut) {
        Jeu jeu = jeuRepository.findById(jeuId)
                .orElseThrow(() ->
                        new NotFoundException("Jeu introuvable"));

        Bibliotheque entree = bibliothequeRepository
                .findByUtilisateurIdAndJeuId(
                        utilisateur.getId(), jeuId)
                .orElseGet(Bibliotheque::new);

        entree.setUtilisateur(utilisateur);
        entree.setJeu(jeu);
        entree.setStatut(statut);
        entree.setDate(LocalDate.now());
        Bibliotheque saved = bibliothequeRepository.save(entree);
        return ResponseMapper.toBibliothequeDto(
                bibliothequeRepository.save(entree));
    }

    // Changer le statut d'un jeu
    @Transactional
    public BibliothequeDto changerStatut(Utilisateur utilisateur,
                                         Long jeuId,
                                         StatutJeu nouveauStatut) {
        Bibliotheque entree = bibliothequeRepository
                .findByUtilisateurIdAndJeuId(
                        utilisateur.getId(), jeuId)
                .orElseThrow(() ->
                        new NotFoundException("Jeu non trouvé dans la bibliothèque"));
        entree.setStatut(nouveauStatut);
        return ResponseMapper.toBibliothequeDto(
                bibliothequeRepository.save(entree));
    }


    // Toute la bibliothèque d'un user
    public List<BibliothequeDto> getBibliotheque(Long utilisateurId) {
        return bibliothequeRepository
                .findByUtilisateurId(utilisateurId)
                .stream()
                .map(ResponseMapper::toBibliothequeDto)
                .toList();
    }

    // Filtrer par statut
    public List<BibliothequeDto> getBibliothequeParStatut(
            Long utilisateurId, StatutJeu statut) {
        return bibliothequeRepository
                .findByUtilisateurIdAndStatut(utilisateurId, statut)
                .stream()
                .map(ResponseMapper::toBibliothequeDto)
                .toList();
    }

    public void supprimerJeu(Utilisateur utilisateur,
                             Long jeuId) {
        Bibliotheque entree = bibliothequeRepository
                .findByUtilisateurIdAndJeuId(
                        utilisateur.getId(), jeuId)
                .orElseThrow(() ->
                        new NotFoundException("Entrée introuvable"));
        bibliothequeRepository.delete(entree);
    }
}