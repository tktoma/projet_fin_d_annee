package com.example.back.service;

import com.example.back.dto.AvisDto;
import com.example.back.dto.ResponseMapper;
import com.example.back.entities.*;
import com.example.back.exception.ConflictException;
import com.example.back.exception.ForbiddenException;
import com.example.back.exception.NotFoundException;
import com.example.back.repository.AvisReactionRepository;
import com.example.back.repository.AvisRepository;
import com.example.back.repository.JeuRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AvisService {

    private final AvisRepository avisRepository;
    private final JeuRepository jeuRepository;
    private final AvisReactionRepository avisReactionRepository;

    public AvisService(AvisRepository avisRepository,
                       JeuRepository jeuRepository,
                       AvisReactionRepository avisReactionRepository) {
        this.avisRepository = avisRepository;
        this.jeuRepository = jeuRepository;
        this.avisReactionRepository = avisReactionRepository;
    }

    public AvisDto ajouterAvis(Utilisateur utilisateur,
                               Long jeuId, String texte) {
        Jeu jeu = jeuRepository.findById(jeuId)
                .orElseThrow(() ->
                        new NotFoundException("Jeu introuvable"));

        Avis avis = avisRepository
                .findByUtilisateurIdAndJeuId(
                        utilisateur.getId(), jeuId)
                .orElse(Avis.builder()
                        .likes(0)
                        .dislikes(0)
                        .build());

        avis.setUtilisateur(utilisateur);
        avis.setJeu(jeu);
        avis.setTexte(texte);
        avis.setDate(LocalDate.now());
        return ResponseMapper.toAvisDto(avisRepository.save(avis));
    }

    /**
     * Like ou dislike un avis.
     * Un utilisateur ne peut réagir qu'une seule fois par avis.
     * S'il a déjà réagi avec le même type → ConflictException.
     * S'il change de type (like → dislike ou inverse) → mise à jour.
     */
    @Transactional
    public AvisDto likerAvis(Long avisId,
                             boolean like,
                             Utilisateur utilisateur) {
        Avis avis = avisRepository.findById(avisId)
                .orElseThrow(() ->
                        new NotFoundException("Avis introuvable"));

        ReactionType nouveauType = like
                ? ReactionType.LIKE
                : ReactionType.DISLIKE;

        avisReactionRepository
                .findByUtilisateurIdAndAvisId(
                        utilisateur.getId(), avisId)
                .ifPresentOrElse(
                        reaction -> {
                            if (reaction.getType() == nouveauType) {
                                throw new ConflictException(
                                        "Vous avez déjà " + (like ? "liké" : "disliké")
                                                + " cet avis");
                            }
                            // Changement de type : annule l'ancien, applique le nouveau
                            if (reaction.getType() == ReactionType.LIKE) {
                                avis.setLikes(avis.getLikes() - 1);
                                avis.setDislikes(avis.getDislikes() + 1);
                            } else {
                                avis.setDislikes(avis.getDislikes() - 1);
                                avis.setLikes(avis.getLikes() + 1);
                            }
                            reaction.setType(nouveauType);
                            avisReactionRepository.save(reaction);
                        },
                        () -> {
                            // Première réaction
                            if (like) avis.setLikes(avis.getLikes() + 1);
                            else avis.setDislikes(avis.getDislikes() + 1);

                            AvisReaction reaction = new AvisReaction();
                            reaction.setUtilisateur(utilisateur);
                            reaction.setAvis(avis);
                            reaction.setType(nouveauType);
                            avisReactionRepository.save(reaction);
                        }
                );

        return ResponseMapper.toAvisDto(avisRepository.save(avis));
    }

    public List<AvisDto> getAvisDuJeu(Long jeuId) {
        return avisRepository.findByJeuId(jeuId)
                .stream()
                .map(ResponseMapper::toAvisDto)
                .toList();
    }

    public List<AvisDto> getAvisDuJeuPages(Long jeuId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return avisRepository.findByJeuIdOrderByDateDesc(jeuId, pageable)
                .getContent()
                .stream()
                .map(ResponseMapper::toAvisDto)
                .toList();
    }

    public List<AvisDto> getMesAvis(Long utilisateurId) {
        return avisRepository.findByUtilisateurId(utilisateurId)
                .stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .map(ResponseMapper::toAvisDto)
                .toList();
    }

    public void supprimerAvis(Utilisateur utilisateur,
                              Long avisId) {
        Avis avis = avisRepository.findById(avisId)
                .orElseThrow(() ->
                        new NotFoundException("Avis introuvable"));
        if (!avis.getUtilisateur().getId().equals(utilisateur.getId())) {
            throw new ForbiddenException("Non autorisé");
        }
        avisRepository.delete(avis);
    }

    public void supprimerAvisAdmin(Long avisId) {
        Avis avis = avisRepository.findById(avisId)
                .orElseThrow(() ->
                        new NotFoundException("Avis introuvable"));
        avisRepository.delete(avis);
    }
}