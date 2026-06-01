package com.example.back.repository;

import com.example.back.entities.Jeu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JeuRepository
        extends JpaRepository<Jeu, Long>,
        JpaSpecificationExecutor<Jeu> {

    boolean existsByExternalId(String externalId);
    Optional<Jeu> findByExternalId(String externalId);
    List<Jeu> findByTitreContainingIgnoreCase(String titre);

    @Query("SELECT DISTINCT j.genre FROM Jeu j WHERE j.genre IS NOT NULL ORDER BY j.genre")
    List<String> findDistinctGenres();

    @Query("SELECT DISTINCT j.plateforme FROM Jeu j WHERE j.plateforme IS NOT NULL ORDER BY j.plateforme")
    List<String> findDistinctPlateformes();

    /** Incrémente le compteur de vues */
    @Modifying
    @Query("UPDATE Jeu j SET j.vues = j.vues + 1 WHERE j.id = :id")
    void incrementVues(@Param("id") Long id);

    /** Nombre de personnes ayant ce jeu en bibliothèque */
    @Query("SELECT COUNT(b) FROM Bibliotheque b WHERE b.jeu.id = :jeuId")
    long countBibliotheque(@Param("jeuId") Long jeuId);

    /** Répartition des statuts pour un jeu */
    @Query("SELECT b.statut, COUNT(b) FROM Bibliotheque b WHERE b.jeu.id = :jeuId GROUP BY b.statut")
    List<Object[]> countParStatut(@Param("jeuId") Long jeuId);
}