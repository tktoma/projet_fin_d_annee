package com.example.back.service;

import com.example.back.dto.ReportRequest;
import com.example.back.dto.ReportResponse;
import com.example.back.dto.ResponseMapper;
import com.example.back.dto.TraiterReportRequest;
import com.example.back.entities.Report;
import com.example.back.entities.StatutReport;
import com.example.back.entities.TypeContenu;
import com.example.back.entities.Utilisateur;
import com.example.back.exception.NotFoundException;
import com.example.back.repository.ReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    // Soumettre un report — accessible à tout utilisateur connecté
    public ReportResponse soumettre(Utilisateur auteur,
                                    ReportRequest request) {

        // Empêcher les doublons : un user ne peut pas signaler
        // deux fois le même contenu
        if (reportRepository
                .existsByAuteurIdAndTypeContenuAndIdContenu(
                        auteur.getId(),
                        request.getTypeContenu(),
                        request.getIdContenu())) {
            throw new RuntimeException(
                    "Vous avez déjà signalé ce contenu");
        }

        Report report = new Report();
        report.setAuteur(auteur);
        report.setTypeContenu(request.getTypeContenu());
        report.setIdContenu(request.getIdContenu());
        report.setRaison(request.getRaison());
        report.setDetails(request.getDetails());
        report.setDate(LocalDateTime.now());
        report.setStatut(StatutReport.EN_ATTENTE);

        return ResponseMapper.toReportResponse(
                reportRepository.save(report));
    }

    // Lister tous les reports — admin uniquement
    public List<ReportResponse> listerTous() {
        return reportRepository.findAllByOrderByDateDesc()
                .stream()
                .map(ResponseMapper::toReportResponse)
                .toList();
    }

    // Filtrer par statut — admin uniquement
    public List<ReportResponse> listerParStatut(
            StatutReport statut) {
        return reportRepository.findByStatut(statut)
                .stream()
                .map(ResponseMapper::toReportResponse)
                .toList();
    }

    // Filtrer par type de contenu — admin uniquement
    public List<ReportResponse> listerParType(
            TypeContenu typeContenu) {
        return reportRepository.findByTypeContenu(typeContenu)
                .stream()
                .map(ResponseMapper::toReportResponse)
                .toList();
    }

    // Traiter un report — admin uniquement
    public ReportResponse traiter(Long reportId,
                                  Utilisateur moderateur,
                                  TraiterReportRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() ->
                        new NotFoundException("Report introuvable"));

        report.setStatut(request.getStatut());
        report.setModerateur(moderateur);
        report.setNoteModerateur(request.getNote());

        return ResponseMapper.toReportResponse(
                reportRepository.save(report));
    }

    // Supprimer un report — admin uniquement
    public void supprimer(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() ->
                        new NotFoundException("Report introuvable"));
        reportRepository.delete(report);
    }
}