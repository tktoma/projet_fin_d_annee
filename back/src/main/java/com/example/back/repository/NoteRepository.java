package com.example.back.repository;

import com.example.back.entities.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository
        extends JpaRepository<Note, Long> {

    List<Note> findByJeuId(Long jeuId);
    List<Note> findByUtilisateurId(Long utilisateurId);
    Optional<Note> findByUtilisateurIdAndJeuId(
            Long utilisateurId, Long jeuId);
    long countByUtilisateurId(Long utilisateurId);

    @Query("SELECT AVG(n.valeur) FROM Note n WHERE n.jeu.id = :jeuId")
    Optional<Float> calculerMoyenne(@Param("jeuId") Long jeuId);

    Page<Note> findByUtilisateurIdOrderByDateDesc(Long utilisateurId, Pageable pageable);

    // Pagination pour getNotesDuJeu — évite les réponses trop lourdes
    Page<Note> findByJeuIdOrderByDateDesc(Long jeuId, Pageable pageable);
}