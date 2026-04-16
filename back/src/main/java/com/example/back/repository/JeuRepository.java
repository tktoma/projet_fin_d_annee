package com.example.back.repository;

import com.example.back.entities.Jeu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JeuRepository
        extends JpaRepository<Jeu, Long> {

    // Vérifie si un jeu IGDB est déjà importé
    boolean existsByExternalId(String externalId);

    // Recherche par titre (insensible à la casse)
    List<Jeu> findByTitreContainingIgnoreCase(String titre);

    // Tous les jeux d'un genre
    List<Jeu> findByGenre(String genre);

    Page<Jeu> findAll(Pageable pageable);
    Page<Jeu> findByTitreContaining(String titre, Pageable pageable);
}
