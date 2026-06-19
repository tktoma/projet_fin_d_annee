package com.example.back.service;

import com.example.back.dto.AvisDto;
import com.example.back.entities.*;
import com.example.back.exception.ConflictException;
import com.example.back.exception.ForbiddenException;
import com.example.back.exception.NotFoundException;
import com.example.back.repository.AvisReactionRepository;
import com.example.back.repository.AvisRepository;
import com.example.back.repository.JeuRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvisServiceTest {

    @Mock AvisRepository avisRepository;
    @Mock JeuRepository jeuRepository;
    @Mock AvisReactionRepository avisReactionRepository;

    @InjectMocks AvisService avisService;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private Utilisateur utilisateur() {
        Utilisateur u = new Utilisateur();
        u.setId(1L); u.setPseudo("alice"); u.setEmail("alice@test.com");
        u.setMdp("hashed"); u.setDateCompte(LocalDate.now()); u.setRole(Role.USER);
        return u;
    }

    private Jeu jeu() {
        Jeu j = new Jeu();
        j.setId(10L); j.setTitre("Dark Souls"); j.setSource("igdb"); j.setNoteMoyenne(0f);
        return j;
    }

    private Avis avis(Utilisateur u, Jeu j) {
        return Avis.builder()
                .utilisateur(u).jeu(j).texte("Super jeu !").likes(0).dislikes(0)
                .date(LocalDate.now()).build();
    }

    // ── ajouterAvis ──────────────────────────────────────────────────────────

    @Nested @DisplayName("ajouterAvis()")
    class AjouterAvis {

        @Test @DisplayName("crée un avis quand le jeu existe et l'utilisateur n'a pas encore écrit")
        void succes_creation() {
            Utilisateur u = utilisateur(); Jeu j = jeu();
            when(jeuRepository.findById(10L)).thenReturn(Optional.of(j));
            when(avisRepository.findByUtilisateurIdAndJeuId(1L, 10L)).thenReturn(Optional.empty());
            when(avisRepository.save(any())).thenAnswer(inv -> {
                Avis a = inv.getArgument(0);
                a.setId(99L); return a; // simule l'id auto-généré
            });

            AvisDto dto = avisService.ajouterAvis(u, 10L, "Super jeu !");

            assertThat(dto.getTexte()).isEqualTo("Super jeu !");
            assertThat(dto.getJeuTitre()).isEqualTo("Dark Souls");
            assertThat(dto.getUtilisateurPseudo()).isEqualTo("alice");
        }

        @Test @DisplayName("met à jour l'avis existant au lieu d'en créer un second")
        void mise_a_jour_si_avis_existant() {
            Utilisateur u = utilisateur(); Jeu j = jeu();
            Avis existant = avis(u, j); existant.setId(5L); existant.setTexte("Ancien texte");
            when(jeuRepository.findById(10L)).thenReturn(Optional.of(j));
            when(avisRepository.findByUtilisateurIdAndJeuId(1L, 10L)).thenReturn(Optional.of(existant));
            when(avisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AvisDto dto = avisService.ajouterAvis(u, 10L, "Nouveau texte");

            assertThat(dto.getTexte()).isEqualTo("Nouveau texte");
            verify(avisRepository, times(1)).save(any()); // pas de création en double
        }

        @Test @DisplayName("lève NotFoundException si le jeu est introuvable")
        void jeu_introuvable() {
            when(jeuRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> avisService.ajouterAvis(utilisateur(), 999L, "texte"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Jeu introuvable");
        }
    }

    // ── likerAvis ────────────────────────────────────────────────────────────

    @Nested @DisplayName("likerAvis()")
    class LikerAvis {

        @Test @DisplayName("incrémente les likes pour une première réaction")
        void premiere_reaction_like() {
            Utilisateur u = utilisateur(); Jeu j = jeu();
            Avis a = avis(u, j); a.setId(5L);
            when(avisRepository.findById(5L)).thenReturn(Optional.of(a));
            when(avisReactionRepository.findByUtilisateurIdAndAvisId(1L, 5L)).thenReturn(Optional.empty());
            when(avisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AvisDto dto = avisService.likerAvis(5L, true, u);

            assertThat(dto.getLikes()).isEqualTo(1);
            assertThat(dto.getDislikes()).isEqualTo(0);
        }

        @Test @DisplayName("lève ConflictException si déjà liké")
        void deja_like() {
            Utilisateur u = utilisateur(); Jeu j = jeu();
            Avis a = avis(u, j); a.setId(5L);
            AvisReaction reaction = new AvisReaction(); reaction.setType(ReactionType.LIKE);
            when(avisRepository.findById(5L)).thenReturn(Optional.of(a));
            when(avisReactionRepository.findByUtilisateurIdAndAvisId(1L, 5L))
                    .thenReturn(Optional.of(reaction));

            assertThatThrownBy(() -> avisService.likerAvis(5L, true, u))
                    .isInstanceOf(ConflictException.class);
        }

        @Test @DisplayName("bascule like→dislike si changement de type")
        void basculement_like_vers_dislike() {
            Utilisateur u = utilisateur(); Jeu j = jeu();
            Avis a = avis(u, j); a.setId(5L); a.setLikes(3); a.setDislikes(1);
            AvisReaction reaction = new AvisReaction(); reaction.setType(ReactionType.LIKE);
            when(avisRepository.findById(5L)).thenReturn(Optional.of(a));
            when(avisReactionRepository.findByUtilisateurIdAndAvisId(1L, 5L))
                    .thenReturn(Optional.of(reaction));
            when(avisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AvisDto dto = avisService.likerAvis(5L, false, u); // passe en dislike

            assertThat(dto.getLikes()).isEqualTo(2);
            assertThat(dto.getDislikes()).isEqualTo(2);
        }
    }

    // ── supprimerAvis ────────────────────────────────────────────────────────

    @Nested @DisplayName("supprimerAvis()")
    class SupprimerAvis {

        @Test @DisplayName("supprime l'avis quand l'utilisateur est le propriétaire")
        void succes() {
            Utilisateur u = utilisateur(); Jeu j = jeu();
            Avis a = avis(u, j); a.setId(7L);
            when(avisRepository.findById(7L)).thenReturn(Optional.of(a));

            avisService.supprimerAvis(u, 7L);

            verify(avisRepository).delete(a);
        }

        @Test @DisplayName("lève ForbiddenException si un autre utilisateur tente de supprimer")
        void acces_interdit() {
            Utilisateur auteur = utilisateur();
            Utilisateur autre = new Utilisateur(); autre.setId(99L);
            Jeu j = jeu();
            Avis a = avis(auteur, j); a.setId(7L);
            when(avisRepository.findById(7L)).thenReturn(Optional.of(a));

            assertThatThrownBy(() -> avisService.supprimerAvis(autre, 7L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Non autorisé");
            verify(avisRepository, never()).delete(any());
        }
    }
}