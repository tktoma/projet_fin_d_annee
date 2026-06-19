package com.example.back.service;

import com.example.back.dto.ReportRequest;
import com.example.back.dto.ReportResponse;
import com.example.back.dto.TraiterReportRequest;
import com.example.back.entities.*;
import com.example.back.exception.NotFoundException;
import com.example.back.repository.ReportRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock ReportRepository reportRepository;
    @InjectMocks ReportService reportService;

    private Utilisateur user(Long id) {
        Utilisateur u = new Utilisateur();
        u.setId(id); u.setPseudo("user" + id); u.setEmail(id + "@t.com");
        u.setMdp("h"); u.setDateCompte(LocalDate.now()); u.setRole(Role.USER);
        return u;
    }

    private Report buildReport(Long id, Utilisateur auteur) {
        Report r = new Report();
        r.setId(id); r.setAuteur(auteur);
        r.setTypeContenu(TypeContenu.JEU); r.setIdContenu(10L);
        r.setRaison(RaisonReport.SPAM); r.setDate(LocalDateTime.now());
        r.setStatut(StatutReport.EN_ATTENTE);
        return r;
    }

    // ── soumettre ─────────────────────────────────────────────────────────────

    @Nested @DisplayName("soumettre()")
    class Soumettre {

        @Test @DisplayName("crée un report correctement")
        void succes() {
            Utilisateur u = user(1L);
            ReportRequest req = new ReportRequest();
            req.setTypeContenu(TypeContenu.JEU);
            req.setIdContenu(10L);
            req.setRaison(RaisonReport.SPAM);
            req.setDetails("Du spam ici");

            when(reportRepository.existsByAuteurIdAndTypeContenuAndIdContenu(1L, TypeContenu.JEU, 10L))
                    .thenReturn(false);
            when(reportRepository.save(any())).thenAnswer(inv -> {
                Report r = inv.getArgument(0); r.setId(99L); return r;
            });

            ReportResponse resp = reportService.soumettre(u, req);

            assertThat(resp.getAuteurPseudo()).isEqualTo("user1");
            assertThat(resp.getRaison()).isEqualTo(RaisonReport.SPAM);
            assertThat(resp.getStatut()).isEqualTo(StatutReport.EN_ATTENTE);
        }

        @Test @DisplayName("lève RuntimeException si doublon")
        void doublon_interdit() {
            Utilisateur u = user(1L);
            ReportRequest req = new ReportRequest();
            req.setTypeContenu(TypeContenu.JEU);
            req.setIdContenu(10L);
            req.setRaison(RaisonReport.SPAM);
            when(reportRepository.existsByAuteurIdAndTypeContenuAndIdContenu(1L, TypeContenu.JEU, 10L))
                    .thenReturn(true);

            assertThatThrownBy(() -> reportService.soumettre(u, req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("déjà signalé");
            verify(reportRepository, never()).save(any());
        }
    }

    // ── listerTous ────────────────────────────────────────────────────────────

    @Test @DisplayName("listerTous() retourne tous les reports triés par date desc")
    void lister_tous() {
        Utilisateur u = user(1L);
        when(reportRepository.findAllByOrderByDateDesc())
                .thenReturn(List.of(buildReport(1L, u), buildReport(2L, u)));

        List<ReportResponse> list = reportService.listerTous();
        assertThat(list).hasSize(2);
    }

    // ── traiter ───────────────────────────────────────────────────────────────

    @Test @DisplayName("traiter() met à jour statut et modérateur")
    void traiter_succes() {
        Utilisateur moderateur = user(2L); moderateur.setRole(Role.ADMIN);
        Report r = buildReport(1L, user(1L));
        when(reportRepository.findById(1L)).thenReturn(Optional.of(r));
        when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TraiterReportRequest req = new TraiterReportRequest();
        req.setStatut(StatutReport.RESOLU);
        req.setNote("Contenu supprimé");

        ReportResponse resp = reportService.traiter(1L, moderateur, req);

        assertThat(resp.getStatut()).isEqualTo(StatutReport.RESOLU);
        assertThat(resp.getModerateurPseudo()).isEqualTo("user2");
        assertThat(resp.getNoteModerateur()).isEqualTo("Contenu supprimé");
    }

    @Test @DisplayName("traiter() lève NotFoundException si report introuvable")
    void traiter_introuvable() {
        when(reportRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reportService.traiter(999L, user(2L), new TraiterReportRequest()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Report introuvable");
    }

    // ── supprimer ─────────────────────────────────────────────────────────────

    @Test @DisplayName("supprimer() efface le report existant")
    void supprimer_succes() {
        Utilisateur u = user(1L);
        Report r = buildReport(5L, u);
        when(reportRepository.findById(5L)).thenReturn(Optional.of(r));

        reportService.supprimer(5L);

        verify(reportRepository).delete(r);
    }

    @Test @DisplayName("supprimer() lève NotFoundException si inexistant")
    void supprimer_introuvable() {
        when(reportRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reportService.supprimer(99L))
                .isInstanceOf(NotFoundException.class);
    }
}