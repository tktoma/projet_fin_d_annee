package com.example.back.repository;

import com.example.back.entities.Avis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AvisRepository
        extends JpaRepository<Avis, Long> {

    List<Avis> findByJeuId(Long jeuId);
    List<Avis> findByUtilisateurId(Long utilisateurId);
    Optional<Avis> findByUtilisateurIdAndJeuId(
            Long utilisateurId, Long jeuId);
    long countByUtilisateurId(Long utilisateurId);
    Page<Avis> findByUtilisateurIdOrderByDateDesc(Long utilisateurId, Pageable pageable);

    // Pagination pour getAvisDuJeu — évite les réponses trop lourdes
    Page<Avis> findByJeuIdOrderByDateDesc(Long jeuId, Pageable pageable);
}