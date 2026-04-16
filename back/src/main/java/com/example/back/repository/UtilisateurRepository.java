package com.example.back.repository;

import com.example.back.entities.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UtilisateurRepository
        extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmail(String email);
    Optional<Utilisateur> findByPseudo(String pseudo);
    boolean existsByEmail(String email);
    boolean existsByPseudo(String pseudo);
    Optional<Utilisateur> findByRefreshToken(String refreshToken);
}