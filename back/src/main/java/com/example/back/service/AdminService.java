package com.example.back.service;

import com.example.back.entities.Avis;
import com.example.back.entities.Role;
import com.example.back.entities.Utilisateur;
import com.example.back.repository.AvisRepository;
import com.example.back.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    private final UtilisateurRepository utilisateurRepository;
    private final AvisRepository avisRepository;

    public AdminService(
            UtilisateurRepository utilisateurRepository,
            AvisRepository avisRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.avisRepository = avisRepository;
    }

    // Changer le rôle d'un utilisateur
    public Utilisateur changerRole(Long utilisateurId,
                                   Role nouveauRole,
                                   Utilisateur demandeur) {
        // Seul un SUPERADMIN peut attribuer SUPERADMIN
        if (nouveauRole == Role.SUPERADMIN
                && demandeur.getRole() != Role.SUPERADMIN) {
            throw new RuntimeException(
                    "Seul un SUPERADMIN peut créer un SUPERADMIN");
        }

        Utilisateur cible = utilisateurRepository
                .findById(utilisateurId)
                .orElseThrow(() ->
                        new RuntimeException("Utilisateur introuvable"));

        // Un admin ne peut pas modifier un autre admin/superadmin
        if (demandeur.getRole() == Role.ADMIN
                && (cible.getRole() == Role.ADMIN
                || cible.getRole() == Role.SUPERADMIN)) {
            throw new RuntimeException("Non autorisé");
        }

        cible.setRole(nouveauRole);
        return utilisateurRepository.save(cible);
    }

    // Liste tous les utilisateurs
    public List<Utilisateur> listerUtilisateurs() {
        return utilisateurRepository.findAll();
    }

    // Supprimer un utilisateur
    public void supprimerUtilisateur(Long utilisateurId,
                                     Utilisateur demandeur) {
        Utilisateur cible = utilisateurRepository
                .findById(utilisateurId)
                .orElseThrow(() ->
                        new RuntimeException("Utilisateur introuvable"));

        if (cible.getRole() == Role.SUPERADMIN) {
            throw new RuntimeException(
                    "Impossible de supprimer un SUPERADMIN");
        }
        utilisateurRepository.delete(cible);
    }

    // Supprimer un avis (modération)
    public void supprimerAvis(Long avisId) {
        Avis avis = avisRepository.findById(avisId)
                .orElseThrow(() ->
                        new RuntimeException("Avis introuvable"));
        avisRepository.delete(avis);
    }
}