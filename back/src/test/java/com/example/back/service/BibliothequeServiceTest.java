package com.example.back.service;

import com.example.back.dto.BibliothequeDto;
import com.example.back.entities.*;
import com.example.back.exception.ConflictException;
import com.example.back.exception.NotFoundException;
import com.example.back.repository.BibliothequeRepository;
import com.example.back.repository.JeuRepository;
import com.example.back.repository.NoteRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BibliothequeServiceTest {

    @Mock BibliothequeRepository bibliothequeRepository;
    @Mock JeuRepository jeuRepository;
    @Mock NoteRepository noteRepository;

    @InjectMocks BibliothequeService bibliothequeService;

    private Utilisateur utilisateur() {
        Utilisateur u = new Utilisateur();
        u.setId(1L); u.setPseudo("alice"); u.setEmail("alice@test.com");
        u.setMdp("h"); u.setDateCompte(LocalDate.now()); u.setRole(Role.USER);
        return u;
    }

    private Jeu jeu() {
        Jeu j = new Jeu(); j.setId(10L); j.setTitre("God of War"); j.setSource("igdb"); j.setNoteMoyenne(0f);
        return j;
    }

    // ── ajouterJeu ──────────────────────────────────────────────────────────

    @Nested @DisplayName("ajouterJeu()")
    class AjouterJeu {

        @Test @DisplayName("ajoute le jeu avec le statut demandé")
        void succes() {
            Utilisateur u = utilisateur(); Jeu j = jeu();
            when(bibliothequeRepository.existsByUtilisateurIdAndJeuId(1L, 10L)).thenReturn(false);
            when(jeuRepository.findById(10L)).thenReturn(Optional.of(j));
            when(bibliothequeRepository.save(any())).thenAnswer(inv -> {
                Bibliotheque b = inv.getArgument(0); b.setId(100L); return b;
            });
            when(noteRepository.findByUtilisateurIdAndJeuId(1L, 10L)).thenReturn(Optional.empty());

            BibliothequeDto dto = bibliothequeService.ajouterJeu(u, 10L, StatutJeu.A_JOUER);

            assertThat(dto.getStatut()).isEqualTo(StatutJeu.A_JOUER);
            assertThat(dto.getJeuTitre()).isEqualTo("God of War");
        }

        @Test @DisplayName("lève ConflictException si déjà en bibliothèque")
        void deja_present() {
            when(bibliothequeRepository.existsByUtilisateurIdAndJeuId(1L, 10L)).thenReturn(true);

            assertThatThrownBy(() -> bibliothequeService.ajouterJeu(utilisateur(), 10L, StatutJeu.JOUER))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("déjà dans votre bibliothèque");
            verify(bibliothequeRepository, never()).save(any());
        }

        @Test @DisplayName("lève NotFoundException si le jeu n'existe pas")
        void jeu_introuvable() {
            when(bibliothequeRepository.existsByUtilisateurIdAndJeuId(1L, 99L)).thenReturn(false);
            when(jeuRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bibliothequeService.ajouterJeu(utilisateur(), 99L, StatutJeu.JOUER))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ── changerStatut ────────────────────────────────────────────────────────

    @Test @DisplayName("changerStatut() met à jour le statut correctement")
    void changer_statut_succes() {
        Utilisateur u = utilisateur(); Jeu j = jeu();
        Bibliotheque b = new Bibliotheque(u, j, StatutJeu.A_JOUER, LocalDate.now()); b.setId(5L);
        when(bibliothequeRepository.findByUtilisateurIdAndJeuId(1L, 10L)).thenReturn(Optional.of(b));
        when(bibliothequeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(noteRepository.findByUtilisateurIdAndJeuId(1L, 10L)).thenReturn(Optional.empty());

        BibliothequeDto dto = bibliothequeService.changerStatut(u, 10L, StatutJeu.FINIT);

        assertThat(dto.getStatut()).isEqualTo(StatutJeu.FINIT);
    }

    // ── supprimerJeu ─────────────────────────────────────────────────────────

    @Test @DisplayName("supprimerJeu() supprime l'entrée existante")
    void supprimer_succes() {
        Utilisateur u = utilisateur(); Jeu j = jeu();
        Bibliotheque b = new Bibliotheque(u, j, StatutJeu.JOUER, LocalDate.now()); b.setId(5L);
        when(bibliothequeRepository.findByUtilisateurIdAndJeuId(1L, 10L)).thenReturn(Optional.of(b));

        bibliothequeService.supprimerJeu(u, 10L);

        verify(bibliothequeRepository).delete(b);
    }

    // ── getBibliotheque ───────────────────────────────────────────────────────

    @Test @DisplayName("getBibliotheque() inclut la note de l'utilisateur quand elle existe")
    void get_bibliotheque_avec_note() {
        Utilisateur u = utilisateur(); Jeu j = jeu();
        Bibliotheque b = new Bibliotheque(u, j, StatutJeu.FINIT, LocalDate.now()); b.setId(5L);
        Note n = new Note(); n.setId(3L); n.setUtilisateur(u); n.setJeu(j);
        n.setValeur(9.5f); n.setDate(LocalDate.now());
        when(bibliothequeRepository.findByUtilisateurId(1L)).thenReturn(List.of(b));
        when(noteRepository.findByUtilisateurIdAndJeuId(1L, 10L)).thenReturn(Optional.of(n));

        List<BibliothequeDto> result = bibliothequeService.getBibliotheque(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMaNote()).isEqualTo(9.5f);
    }
}