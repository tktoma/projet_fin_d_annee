package com.example.back.service;

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

    private final AvisRepository avisRepository;
    private final JeuRepository jeuRepository;

    public AvisService(AvisRepository avisRepository,
                       JeuRepository jeuRepository) {
        this.avisRepository = avisRepository;
        this.jeuRepository = jeuRepository;
    }

    public Avis ajouterAvis(Utilisateur utilisateur,
                            Long jeuId, String texte) {
        Jeu jeu = jeuRepository.findById(jeuId)
                .orElseThrow(() ->
                        new RuntimeException("Jeu introuvable"));

        Avis avis = avisRepository
                .findByUtilisateurIdAndJeuId(
                        utilisateur.getId(), jeuId)
                .orElse(new Avis());

        avis.setUtilisateur(utilisateur);
        avis.setJeu(jeu);
        avis.setTexte(texte);
        avis.setDate(LocalDate.now());
        return avisRepository.save(avis);
    }

    public Avis likerAvis(Long avisId, boolean like) {
        Avis avis = avisRepository.findById(avisId)
                .orElseThrow(() ->
                        new RuntimeException("Avis introuvable"));
        if (like) avis.setLikes(avis.getLikes() + 1);
        else avis.setDislikes(avis.getDislikes() + 1);
        return avisRepository.save(avis);
    }

    public List<Avis> getAvisDuJeu(Long jeuId) {
        return avisRepository.findByJeuId(jeuId);
    }

    public void supprimerAvis(Utilisateur utilisateur,
                              Long avisId) {
        Avis avis = avisRepository.findById(avisId)
                .orElseThrow(() ->
                        new RuntimeException("Avis introuvable"));
        if (avis.getUtilisateur().getId() != utilisateur.getId()) {
            throw new RuntimeException("Non autorisé");
        }
        avisRepository.delete(avis);
    }
}
