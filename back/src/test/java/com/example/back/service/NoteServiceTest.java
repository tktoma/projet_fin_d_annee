package com.example.back.service;

import com.example.back.dto.NoteDto;
import com.example.back.entities.*;
import com.example.back.exception.NotFoundException;
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
class NoteServiceTest {

    @Mock NoteRepository noteRepository;
    @Mock JeuRepository jeuRepository;

    @InjectMocks NoteService noteService;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private Utilisateur utilisateur() {
        Utilisateur u = new Utilisateur();
        u.setId(1L); u.setPseudo("bob"); u.setEmail("bob@test.com");
        u.setMdp("hashed"); u.setDateCompte(LocalDate.now()); u.setRole(Role.USER);
        return u;
    }

    private Jeu jeu() {
        Jeu j = new Jeu(); j.setId(10L); j.setTitre("Zelda"); j.setNoteMoyenne(0f); j.setSource("igdb");
        return j;
    }

    // ── noterJeu ─────────────────────────────────────────────────────────────

    @Nested @DisplayName("noterJeu()")
    class NoterJeu {

        @Test @DisplayName("crée une note et recalcule la moyenne")
        void succes_creation() {
            Utilisateur u = utilisateur(); Jeu j = jeu();
            when(jeuRepository.findById(10L)).thenReturn(Optional.of(j));
            when(noteRepository.findByUtilisateurIdAndJeuId(1L, 10L)).thenReturn(Optional.empty());
            when(noteRepository.save(any())).thenAnswer(inv -> {
                Note n = inv.getArgument(0); n.setId(20L); return n;
            });
            when(noteRepository.calculerMoyenne(10L)).thenReturn(Optional.of(8.5f));
            when(jeuRepository.save(any())).thenReturn(j);

            NoteDto dto = noteService.noterJeu(u, 10L, 8.5f);

            assertThat(dto.getValeur()).isEqualTo(8.5f);
            assertThat(dto.getJeuTitre()).isEqualTo("Zelda");
            assertThat(j.getNoteMoyenne()).isEqualTo(8.5f); // moyenne mise à jour
        }

        @Test @DisplayName("met à jour la note existante")
        void mise_a_jour() {
            Utilisateur u = utilisateur(); Jeu j = jeu();
            Note existante = new Note(); existante.setId(5L);
            existante.setUtilisateur(u); existante.setJeu(j); existante.setValeur(6f);
            existante.setDate(LocalDate.now());
            when(jeuRepository.findById(10L)).thenReturn(Optional.of(j));
            when(noteRepository.findByUtilisateurIdAndJeuId(1L, 10L)).thenReturn(Optional.of(existante));
            when(noteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(noteRepository.calculerMoyenne(10L)).thenReturn(Optional.of(9f));
            when(jeuRepository.save(any())).thenReturn(j);

            NoteDto dto = noteService.noterJeu(u, 10L, 9f);

            assertThat(dto.getValeur()).isEqualTo(9f);
        }

        @Test @DisplayName("lève NotFoundException si jeu introuvable")
        void jeu_introuvable() {
            when(jeuRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> noteService.noterJeu(utilisateur(), 99L, 5f))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ── supprimerNote ────────────────────────────────────────────────────────

    @Nested @DisplayName("supprimerNote()")
    class SupprimerNote {

        @Test @DisplayName("supprime la note et remet la moyenne à 0 s'il n'y en a plus")
        void succes() {
            Utilisateur u = utilisateur(); Jeu j = jeu();
            Note n = new Note(); n.setId(5L); n.setUtilisateur(u); n.setJeu(j); n.setValeur(7f);
            when(noteRepository.findByUtilisateurIdAndJeuId(1L, 10L)).thenReturn(Optional.of(n));
            when(jeuRepository.findById(10L)).thenReturn(Optional.of(j));
            when(noteRepository.calculerMoyenne(10L)).thenReturn(Optional.empty());
            when(jeuRepository.save(any())).thenReturn(j);

            noteService.supprimerNote(u, 10L);

            verify(noteRepository).delete(n);
            assertThat(j.getNoteMoyenne()).isEqualTo(0f);
        }

        @Test @DisplayName("lève NotFoundException si la note n'existe pas")
        void note_introuvable() {
            when(noteRepository.findByUtilisateurIdAndJeuId(1L, 10L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> noteService.supprimerNote(utilisateur(), 10L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Note introuvable");
        }
    }
}