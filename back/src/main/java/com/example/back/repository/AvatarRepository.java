package com.example.back.repository;

import com.example.back.entities.Avatar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AvatarRepository
        extends JpaRepository<Avatar, Long> {

    Optional<Avatar> findByUtilisateurId(Long utilisateurId);
    boolean existsByUtilisateurId(Long utilisateurId);
}
