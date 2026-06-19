package com.example.back.dto;

import com.example.back.entities.*;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires purs — aucune dépendance Spring.
 * Vérifient que ResponseMapper ne perd aucune donnée lors de la conversion.
 */
class ResponseMapperTest {

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private Utilisateur utilisateur() {
        Utilisateur u = new Utilisateur();
        u.setId(1L); u.setPseudo("alice"); u.setEmail("alice@test.com");
        u.setMdp("hashed"); u.setDateCompte(LocalDate.of(2024, 1, 15)); u.setRole(Role.USER);
        return u;
    }

    private Jeu jeu() {
        Jeu j = new Jeu();
        j.setId(10L); j.setTitre("Elden Ring"); j.setGenre("RPG");
        j.setPlateforme("PC"); j.setCoverUrl("https://img.igdb.com/cover.jpg");
        j.setDateSortie(LocalDate.of(2022, 2, 25));
        j.setNoteMoyenne(9.5f); j.setSource("igdb"); j.setExternalId("ext-123");
        j.setDescription("Un action-RPG épique"); j.setVues(1000L);
        return j;
    }

    // ── toUtilisateurResponse ─────────────────────────────────────────────────

    @Test @DisplayName("toUtilisateurResponse() mappe tous les champs")
    void to_utilisateur_response() {
        UtilisateurResponse dto = ResponseMapper.toUtilisateurResponse(utilisateur());
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getPseudo()).isEqualTo("alice");
        assertThat(dto.getEmail()).isEqualTo("alice@test.com");
        assertThat(dto.getRole()).isEqualTo(Role.USER);
        assertThat(dto.getDateCompte()).isEqualTo(LocalDate.of(2024, 1, 15));
    }

    @Test @DisplayName("toUtilisateurResponse() ne lève pas d'exception si rôle null")
    void to_utilisateur_response_role_null() {
        Utilisateur u = utilisateur(); u.setRole(null);
        assertThatNoException().isThrownBy(() -> ResponseMapper.toUtilisateurResponse(u));
    }

    // ── toJeuResponse ─────────────────────────────────────────────────────────

    @Test @DisplayName("toJeuResponse() mappe tous les champs")
    void to_jeu_response() {
        JeuResponse dto = ResponseMapper.toJeuResponse(jeu());
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getTitre()).isEqualTo("Elden Ring");
        assertThat(dto.getGenre()).isEqualTo("RPG");
        assertThat(dto.getPlateforme()).isEqualTo("PC");
        assertThat(dto.getCoverUrl()).isEqualTo("https://img.igdb.com/cover.jpg");
        assertThat(dto.getDateSortie()).isEqualTo(LocalDate.of(2022, 2, 25));
        assertThat(dto.getNoteMoyenne()).isEqualTo(9.5f);
        assertThat(dto.getSource()).isEqualTo("igdb");
        assertThat(dto.getExternalId()).isEqualTo("ext-123");
        assertThat(dto.getDescription()).isEqualTo("Un action-RPG épique");
        assertThat(dto.getVues()).isEqualTo(1000L);
    }

    @Test @DisplayName("toJeuResponse() ne peuple pas nbBibliotheque ni statutStats")
    void to_jeu_response_stats_nulles() {
        JeuResponse dto = ResponseMapper.toJeuResponse(jeu());
        // Ces champs sont enrichis séparément dans IgdbService.enrichWithStats()
        assertThat(dto.getNbBibliotheque()).isEqualTo(0L);
        assertThat(dto.getStatutStats()).isNull();
    }

    // ── toAvisDto ─────────────────────────────────────────────────────────────

    @Test @DisplayName("toAvisDto() mappe likes, dislikes et pseudo auteur")
    void to_avis_dto() {
        Utilisateur u = utilisateur(); Jeu j = jeu();
        Avis avis = Avis.builder()
                .utilisateur(u).jeu(j).texte("Incroyable !").likes(42).dislikes(3)
                .date(LocalDate.of(2024, 6, 1)).build();
        avis.setId(5L);

        AvisDto dto = ResponseMapper.toAvisDto(avis);
        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getJeuId()).isEqualTo(10L);
        assertThat(dto.getJeuTitre()).isEqualTo("Elden Ring");
        assertThat(dto.getUtilisateurId()).isEqualTo(1L);
        assertThat(dto.getUtilisateurPseudo()).isEqualTo("alice");
        assertThat(dto.getTexte()).isEqualTo("Incroyable !");
        assertThat(dto.getLikes()).isEqualTo(42);
        assertThat(dto.getDislikes()).isEqualTo(3);
        assertThat(dto.getDate()).isEqualTo(LocalDate.of(2024, 6, 1));
    }

    // ── toNoteDto ─────────────────────────────────────────────────────────────

    @Test @DisplayName("toNoteDto() mappe la valeur et les références")
    void to_note_dto() {
        Utilisateur u = utilisateur(); Jeu j = jeu();
        Note note = new Note(); note.setId(7L);
        note.setUtilisateur(u); note.setJeu(j);
        note.setValeur(8.5f); note.setDate(LocalDate.of(2024, 3, 10));

        NoteDto dto = ResponseMapper.toNoteDto(note);
        assertThat(dto.getId()).isEqualTo(7L);
        assertThat(dto.getValeur()).isEqualTo(8.5f);
        assertThat(dto.getJeuTitre()).isEqualTo("Elden Ring");
        assertThat(dto.getUtilisateurPseudo()).isEqualTo("alice");
    }

    // ── toBibliothequeDto ─────────────────────────────────────────────────────

    @Test @DisplayName("toBibliothequeDto() mappe statut et jeu correctement")
    void to_bibliotheque_dto() {
        Utilisateur u = utilisateur(); Jeu j = jeu();
        Bibliotheque b = new Bibliotheque(u, j, StatutJeu.FINIT, LocalDate.of(2024, 5, 20));
        b.setId(9L);

        BibliothequeDto dto = ResponseMapper.toBibliothequeDto(b);
        assertThat(dto.getId()).isEqualTo(9L);
        assertThat(dto.getJeuId()).isEqualTo(10L);
        assertThat(dto.getJeuTitre()).isEqualTo("Elden Ring");
        assertThat(dto.getStatut()).isEqualTo(StatutJeu.FINIT);
        assertThat(dto.getDate()).isEqualTo(LocalDate.of(2024, 5, 20));
        assertThat(dto.getMaNote()).isNull(); // enrichi séparément
    }

    // ── toReportResponse ──────────────────────────────────────────────────────

    @Test @DisplayName("toReportResponse() mappe sans modérateur (null-safe)")
    void to_report_response_sans_moderateur() {
        Utilisateur auteur = utilisateur();
        Report r = new Report();
        r.setId(3L); r.setAuteur(auteur);
        r.setTypeContenu(TypeContenu.JEU); r.setIdContenu(10L);
        r.setRaison(RaisonReport.SPAM); r.setDate(LocalDateTime.now());
        r.setStatut(StatutReport.EN_ATTENTE); r.setModerateur(null);

        ReportResponse dto = ResponseMapper.toReportResponse(r);
        assertThat(dto.getModerateurId()).isNull();
        assertThat(dto.getModerateurPseudo()).isNull();
        assertThat(dto.getAuteurPseudo()).isEqualTo("alice");
        assertThat(dto.getStatut()).isEqualTo(StatutReport.EN_ATTENTE);
    }

    @Test @DisplayName("toReportResponse() inclut le modérateur s'il est défini")
    void to_report_response_avec_moderateur() {
        Utilisateur auteur = utilisateur();
        Utilisateur mod = new Utilisateur(); mod.setId(2L); mod.setPseudo("admin");
        mod.setEmail("admin@t.com"); mod.setMdp("h");
        mod.setDateCompte(LocalDate.now()); mod.setRole(Role.ADMIN);

        Report r = new Report();
        r.setId(4L); r.setAuteur(auteur); r.setModerateur(mod);
        r.setTypeContenu(TypeContenu.AVIS); r.setIdContenu(5L);
        r.setRaison(RaisonReport.CONTENU_INAPPROPRIE);
        r.setDate(LocalDateTime.now()); r.setStatut(StatutReport.RESOLU);
        r.setNoteModerateur("Contenu retiré");

        ReportResponse dto = ResponseMapper.toReportResponse(r);
        assertThat(dto.getModerateurId()).isEqualTo(2L);
        assertThat(dto.getModerateurPseudo()).isEqualTo("admin");
        assertThat(dto.getNoteModerateur()).isEqualTo("Contenu retiré");
    }

    // ── toAvatarDto ───────────────────────────────────────────────────────────

    @Test @DisplayName("toAvatarDto() mappe url, contentType et taille")
    void to_avatar_dto() {
        Utilisateur u = utilisateur();
        Avatar a = new Avatar(u, "http://localhost/avatars/img.jpg", "image/jpeg", 512_000L);
        a.setId(1L);

        AvatarDto dto = ResponseMapper.toAvatarDto(a);
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getUtilisateurId()).isEqualTo(1L);
        assertThat(dto.getUrl()).isEqualTo("http://localhost/avatars/img.jpg");
        assertThat(dto.getContentType()).isEqualTo("image/jpeg");
        assertThat(dto.getTaille()).isEqualTo(512_000L);
    }
}