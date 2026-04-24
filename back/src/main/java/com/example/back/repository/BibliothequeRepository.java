package com.example.back.repository;

import com.example.back.entities.Bibliotheque;
import com.example.back.entities.StatutJeu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BibliothequeRepository
        extends JpaRepository<Bibliotheque, Long> {

    List<Bibliotheque> findByUtilisateurId(Long utilisateurId);
    Optional<Bibliotheque> findByUtilisateurIdAndJeuId(
            Long utilisateurId, Long jeuId);
    boolean existsByUtilisateurIdAndJeuId(
            Long utilisateurId, Long jeuId);
    long countByUtilisateurId(Long utilisateurId);
    // Filtrer par statut
    List<Bibliotheque> findByUtilisateurIdAndStatut(
            Long utilisateurId, StatutJeu statut);
}
