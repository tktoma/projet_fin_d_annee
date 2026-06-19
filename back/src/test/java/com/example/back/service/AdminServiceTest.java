package com.example.back.service;

import com.example.back.dto.UtilisateurResponse;
import com.example.back.entities.Role;
import com.example.back.entities.Utilisateur;
import com.example.back.exception.ForbiddenException;
import com.example.back.exception.NotFoundException;
import com.example.back.repository.UtilisateurRepository;
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
class AdminServiceTest {

    @Mock UtilisateurRepository utilisateurRepository;
    @Mock AvisService avisService;

    @InjectMocks AdminService adminService;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private Utilisateur user(Long id, Role role) {
        Utilisateur u = new Utilisateur();
        u.setId(id); u.setPseudo("user" + id); u.setEmail(id + "@test.com");
        u.setMdp("hashed"); u.setDateCompte(LocalDate.now()); u.setRole(role);
        return u;
    }

    // ── changerRole ──────────────────────────────────────────────────────────

    @Nested @DisplayName("changerRole()")
    class ChangerRole {

        @Test @DisplayName("SUPERADMIN peut changer un USER en ADMIN")
        void superadmin_peut_promouvoir() {
            Utilisateur superadmin = user(1L, Role.SUPERADMIN);
            Utilisateur cible = user(2L, Role.USER);
            when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(cible));
            when(utilisateurRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UtilisateurResponse resp = adminService.changerRole(2L, Role.ADMIN, superadmin);

            assertThat(resp.getRole()).isEqualTo(Role.ADMIN);
        }

        @Test @DisplayName("ADMIN ne peut pas promouvoir un autre ADMIN")
        void admin_ne_peut_pas_modifier_admin() {
            Utilisateur admin = user(1L, Role.ADMIN);
            Utilisateur autreAdmin = user(2L, Role.ADMIN);
            when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(autreAdmin));

            assertThatThrownBy(() -> adminService.changerRole(2L, Role.USER, admin))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Non autorisé");
        }

        @Test @DisplayName("seul SUPERADMIN peut créer un autre SUPERADMIN")
        void seul_superadmin_peut_creer_superadmin() {
            Utilisateur admin = user(1L, Role.ADMIN);
            Utilisateur cible = user(2L, Role.USER);
            when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(cible));

            assertThatThrownBy(() -> adminService.changerRole(2L, Role.SUPERADMIN, admin))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Seul un SUPERADMIN peut créer un SUPERADMIN");
        }

        @Test @DisplayName("lève NotFoundException si l'utilisateur cible est introuvable")
        void utilisateur_introuvable() {
            when(utilisateurRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() ->
                    adminService.changerRole(999L, Role.USER, user(1L, Role.SUPERADMIN)))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ── supprimerUtilisateur ─────────────────────────────────────────────────

    @Nested @DisplayName("supprimerUtilisateur()")
    class SupprimerUtilisateur {

        @Test @DisplayName("SUPERADMIN peut supprimer un USER")
        void succes() {
            Utilisateur superadmin = user(1L, Role.SUPERADMIN);
            Utilisateur cible = user(2L, Role.USER);
            when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(cible));

            adminService.supprimerUtilisateur(2L, superadmin);

            verify(utilisateurRepository).delete(cible);
        }

        @Test @DisplayName("impossible de supprimer un SUPERADMIN")
        void ne_peut_pas_supprimer_superadmin() {
            Utilisateur superadmin1 = user(1L, Role.SUPERADMIN);
            Utilisateur superadmin2 = user(2L, Role.SUPERADMIN);
            when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(superadmin2));

            assertThatThrownBy(() -> adminService.supprimerUtilisateur(2L, superadmin1))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Impossible de supprimer un SUPERADMIN");
            verify(utilisateurRepository, never()).delete(any());
        }

        @Test @DisplayName("ADMIN ne peut pas supprimer un autre ADMIN")
        void admin_ne_peut_pas_supprimer_admin() {
            Utilisateur admin1 = user(1L, Role.ADMIN);
            Utilisateur admin2 = user(2L, Role.ADMIN);
            when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(admin2));

            assertThatThrownBy(() -> adminService.supprimerUtilisateur(2L, admin1))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Non autorisé");
        }
    }

    // ── listerUtilisateurs ───────────────────────────────────────────────────

    @Test @DisplayName("listerUtilisateurs() retourne tous les utilisateurs")
    void lister_utilisateurs() {
        when(utilisateurRepository.findAll())
                .thenReturn(List.of(user(1L, Role.USER), user(2L, Role.ADMIN)));

        List<UtilisateurResponse> list = adminService.listerUtilisateurs();

        assertThat(list).hasSize(2);
    }
}