package com.example.back.repository;

import com.example.back.entities.AvisReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AvisReactionRepository
        extends JpaRepository<AvisReaction, Long> {

    Optional<AvisReaction> findByUtilisateurIdAndAvisId(
            Long utilisateurId, Long avisId);

    boolean existsByUtilisateurIdAndAvisId(
            Long utilisateurId, Long avisId);
}