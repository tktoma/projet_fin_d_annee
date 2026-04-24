package com.example.back.repository;

import com.example.back.entities.Jeu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
}
