package com.example.back.repository;

import com.example.back.entities.Report;
import com.example.back.entities.StatutReport;
import com.example.back.entities.TypeContenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository
        extends JpaRepository<Report, Long> {

    List<Report> findAllByOrderByDateDesc();
    List<Report> findByStatut(StatutReport statut);
    List<Report> findByTypeContenu(TypeContenu typeContenu);
    boolean existsByAuteurIdAndTypeContenuAndIdContenu(
            Long auteurId, TypeContenu typeContenu, Long idContenu);
}