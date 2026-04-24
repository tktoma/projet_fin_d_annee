package com.example.back.service;

import com.example.back.dto.ResponseMapper;
import com.example.back.dto.UtilisateurResponse;
import com.example.back.entities.Avis;
import com.example.back.entities.Role;
import com.example.back.entities.Utilisateur;
import com.example.back.exception.ForbiddenException;
import com.example.back.exception.NotFoundException;
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

    public UtilisateurResponse changerRole(Long utilisateurId,
                                           Role nouveauRole,
                                           Utilisateur demandeur) {
        if (nouveauRole == Role.SUPERADMIN
                && demandeur.getRole() != Role.SUPERADMIN) {
            throw new ForbiddenException(
                    "Seul un SUPERADMIN peut créer un SUPERADMIN");
        }

        Utilisateur cible = utilisateurRepository
                .findById(utilisateurId)
                .orElseThrow(() ->
                        new NotFoundException("Utilisateur introuvable"));

        if (demandeur.getRole() == Role.ADMIN
                && (cible.getRole() == Role.ADMIN
                || cible.getRole() == Role.SUPERADMIN)) {
            throw new ForbiddenException("Non autorisé");
        }

        cible.setRole(nouveauRole);
        return ResponseMapper.toUtilisateurResponse(
                utilisateurRepository.save(cible));
    }

    public List<UtilisateurResponse> listerUtilisateurs() {
        return utilisateurRepository.findAll()
                .stream()
                .map(ResponseMapper::toUtilisateurResponse)
                .toList();
    }

    public void supprimerUtilisateur(Long utilisateurId,
                                     Utilisateur demandeur) {
        Utilisateur cible = utilisateurRepository
                .findById(utilisateurId)
                .orElseThrow(() ->
                        new NotFoundException("Utilisateur introuvable"));

        if (cible.getRole() == Role.SUPERADMIN) {
            throw new ForbiddenException(
                    "Impossible de supprimer un SUPERADMIN");
        }
        // Un ADMIN ne peut pas supprimer un autre ADMIN
        if (demandeur.getRole() == Role.ADMIN
                && cible.getRole() == Role.ADMIN) {
            throw new ForbiddenException("Non autorisé");
        }
        utilisateurRepository.delete(cible);
    }

    public void supprimerAvis(Long avisId) {
        Avis avis = avisRepository.findById(avisId)
                .orElseThrow(() ->
                        new RuntimeException("Avis introuvable"));
        avisRepository.delete(avis);
    }
}