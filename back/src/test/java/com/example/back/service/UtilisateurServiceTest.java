package com.example.back.service;

import com.example.back.config.JwtUtil;
import com.example.back.dto.AuthResponse;
import com.example.back.dto.LoginRequest;
import com.example.back.dto.RegisterRequest;
import com.example.back.entities.Utilisateur;
import com.example.back.repository.AvisRepository;
import com.example.back.repository.BibliothequeRepository;
import com.example.back.repository.NoteRepository;
import com.example.back.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtilisateurServiceTest {

    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AvisRepository avisRepository;
    @Mock private NoteRepository noteRepository;
    @Mock private BibliothequeRepository bibliothequeRepository;

    @InjectMocks
    private UtilisateurService utilisateurService;

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private RegisterRequest registerRequest() {
        RegisterRequest r = new RegisterRequest();
        r.setPseudo("alice");
        r.setEmail("alice@example.com");
        r.setMotDePasse("motdepasse123");
        return r;
    }

    private Utilisateur utilisateurSauvegarde() {
        Utilisateur u = new Utilisateur();
        u.setId(1L);
        u.setPseudo("alice");
        u.setEmail("alice@example.com");
        u.setMdp("$2a$hashed");
        u.setDateCompte(LocalDate.now());
        return u;
    }

    // -------------------------------------------------------------------------
    // Inscription
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("inscrire()")
    class Inscription {

        @Test
        @DisplayName("retourne un AuthResponse avec token quand les données sont valides")
        void inscription_succes() {
            when(utilisateurRepository.existsByEmail(anyString()))
                    .thenReturn(false);
            when(utilisateurRepository.existsByPseudo(anyString()))
                    .thenReturn(false);
            when(passwordEncoder.encode(anyString()))
                    .thenReturn("$2a$hashed");
            when(utilisateurRepository.save(any()))
                    .thenReturn(utilisateurSauvegarde());
            when(jwtUtil.generateToken(any()))
                    .thenReturn("jwt-token");
            when(jwtUtil.generateRefreshToken())
                    .thenReturn("refresh-token");

            AuthResponse response = utilisateurService
                    .inscrire(registerRequest());

            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getPseudo()).isEqualTo("alice");
        }

        @Test
        @DisplayName("encode le mot de passe avant de sauvegarder")
        void inscription_encodeLeMotDePasse() {
            when(utilisateurRepository.existsByEmail(anyString()))
                    .thenReturn(false);
            when(utilisateurRepository.existsByPseudo(anyString()))
                    .thenReturn(false);
            when(passwordEncoder.encode("motdepasse123"))
                    .thenReturn("$2a$hashed");
            when(utilisateurRepository.save(any()))
                    .thenReturn(utilisateurSauvegarde());
            when(jwtUtil.generateToken(any())).thenReturn("jwt");
            when(jwtUtil.generateRefreshToken()).thenReturn("refresh");

            utilisateurService.inscrire(registerRequest());

            // Capture l'utilisateur passé à save() et vérifie son mdp
            ArgumentCaptor<Utilisateur> captor =
                    ArgumentCaptor.forClass(Utilisateur.class);
            verify(utilisateurRepository, atLeastOnce()).save(captor.capture());

            assertThat(captor.getAllValues())
                    .anyMatch(u -> "$2a$hashed".equals(u.getMdp()));
        }

        @Test
        @DisplayName("lève une exception si l'email est déjà utilisé")
        void inscription_emailDejaPris() {
            when(utilisateurRepository.existsByEmail("alice@example.com"))
                    .thenReturn(true);

            assertThatThrownBy(() ->
                    utilisateurService.inscrire(registerRequest()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Email déjà utilisé");

            verify(utilisateurRepository, never()).save(any());
        }

        @Test
        @DisplayName("lève une exception si le pseudo est déjà utilisé")
        void inscription_pseudoDejaPris() {
            when(utilisateurRepository.existsByEmail(anyString()))
                    .thenReturn(false);
            when(utilisateurRepository.existsByPseudo("alice"))
                    .thenReturn(true);

            assertThatThrownBy(() ->
                    utilisateurService.inscrire(registerRequest()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Pseudo déjà utilisé");

            verify(utilisateurRepository, never()).save(any());
        }

        @Test
        @DisplayName("persiste le refresh token après inscription")
        void inscription_sauvegardeLeRefreshToken() {
            when(utilisateurRepository.existsByEmail(anyString()))
                    .thenReturn(false);
            when(utilisateurRepository.existsByPseudo(anyString()))
                    .thenReturn(false);
            when(passwordEncoder.encode(anyString()))
                    .thenReturn("$2a$hashed");
            when(utilisateurRepository.save(any()))
                    .thenReturn(utilisateurSauvegarde());
            when(jwtUtil.generateToken(any())).thenReturn("jwt");
            when(jwtUtil.generateRefreshToken()).thenReturn("refresh-token");

            utilisateurService.inscrire(registerRequest());

            ArgumentCaptor<Utilisateur> captor =
                    ArgumentCaptor.forClass(Utilisateur.class);
            verify(utilisateurRepository, atLeastOnce()).save(captor.capture());

            // Le deuxième save() doit porter le refreshToken
            assertThat(captor.getAllValues())
                    .anyMatch(u -> "refresh-token".equals(u.getRefreshToken()));
        }
    }

    // -------------------------------------------------------------------------
    // Connexion
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("connecter()")
    class Connexion {

        private LoginRequest loginRequest() {
            LoginRequest r = new LoginRequest();
            r.setEmail("alice@example.com");
            r.setMotDePasse("motdepasse123");
            return r;
        }

        @Test
        @DisplayName("retourne un AuthResponse quand les identifiants sont corrects")
        void connexion_succes() {
            Utilisateur u = utilisateurSauvegarde();
            when(utilisateurRepository.findByEmail("alice@example.com"))
                    .thenReturn(Optional.of(u));
            when(passwordEncoder.matches("motdepasse123", "$2a$hashed"))
                    .thenReturn(true);
            when(jwtUtil.generateToken(u)).thenReturn("jwt-token");
            when(jwtUtil.generateRefreshToken()).thenReturn("refresh-token");
            when(utilisateurRepository.save(any())).thenReturn(u);

            AuthResponse response = utilisateurService.connecter(loginRequest());

            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getPseudo()).isEqualTo("alice");
            assertThat(response.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("lève une exception si l'email est introuvable")
        void connexion_emailIntrouvable() {
            when(utilisateurRepository.findByEmail("alice@example.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    utilisateurService.connecter(loginRequest()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Email introuvable");
        }

        @Test
        @DisplayName("lève une exception si le mot de passe est incorrect")
        void connexion_mauvaisMotDePasse() {
            Utilisateur u = utilisateurSauvegarde();
            when(utilisateurRepository.findByEmail("alice@example.com"))
                    .thenReturn(Optional.of(u));
            when(passwordEncoder.matches("motdepasse123", "$2a$hashed"))
                    .thenReturn(false);

            assertThatThrownBy(() ->
                    utilisateurService.connecter(loginRequest()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Mot de passe incorrect");

            verify(jwtUtil, never()).generateToken(any());
        }

        @Test
        @DisplayName("renouvelle le refresh token à chaque connexion")
        void connexion_renouvelleLeRefreshToken() {
            Utilisateur u = utilisateurSauvegarde();
            u.setRefreshToken("ancien-refresh");
            when(utilisateurRepository.findByEmail("alice@example.com"))
                    .thenReturn(Optional.of(u));
            when(passwordEncoder.matches(anyString(), anyString()))
                    .thenReturn(true);
            when(jwtUtil.generateToken(any())).thenReturn("jwt");
            when(jwtUtil.generateRefreshToken()).thenReturn("nouveau-refresh");
            when(utilisateurRepository.save(any())).thenReturn(u);

            AuthResponse response = utilisateurService.connecter(loginRequest());

            assertThat(response.getRefreshToken()).isEqualTo("nouveau-refresh");
        }
    }

    // -------------------------------------------------------------------------
    // Refresh token
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("refreshToken()")
    class RefreshToken {

        private Utilisateur utilisateurAvecRefresh(boolean expire) {
            Utilisateur u = utilisateurSauvegarde();
            u.setRefreshToken("refresh-valide");
            u.setRefreshTokenExpiration(expire
                    ? LocalDateTime.now().minusDays(1)   // expiré
                    : LocalDateTime.now().plusDays(29));  // valide
            return u;
        }

        @Test
        @DisplayName("retourne de nouveaux tokens si le refresh token est valide")
        void refresh_succes() {
            Utilisateur u = utilisateurAvecRefresh(false);
            when(jwtUtil.validateRefreshToken("refresh-valide"))
                    .thenReturn(true);
            when(utilisateurRepository.findByRefreshToken("refresh-valide"))
                    .thenReturn(Optional.of(u));
            when(jwtUtil.generateToken(u)).thenReturn("nouveau-jwt");
            when(jwtUtil.generateRefreshToken()).thenReturn("nouveau-refresh");
            when(utilisateurRepository.save(any())).thenReturn(u);

            AuthResponse response = utilisateurService
                    .refreshToken("refresh-valide");

            assertThat(response.getToken()).isEqualTo("nouveau-jwt");
            assertThat(response.getRefreshToken()).isEqualTo("nouveau-refresh");
        }

        @Test
        @DisplayName("lève une exception si le refresh token est invalide (JWT malformé)")
        void refresh_tokenJwtInvalide() {
            when(jwtUtil.validateRefreshToken("token-bidon"))
                    .thenReturn(false);

            assertThatThrownBy(() ->
                    utilisateurService.refreshToken("token-bidon"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Refresh token invalide");

            verify(utilisateurRepository, never()).findByRefreshToken(any());
        }

        @Test
        @DisplayName("lève une exception si le refresh token est introuvable en BDD")
        void refresh_tokenIntrouvableEnBdd() {
            when(jwtUtil.validateRefreshToken("refresh-inconnu"))
                    .thenReturn(true);
            when(utilisateurRepository.findByRefreshToken("refresh-inconnu"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    utilisateurService.refreshToken("refresh-inconnu"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Refresh token introuvable");
        }

        @Test
        @DisplayName("lève une exception si le refresh token est expiré")
        void refresh_tokenExpire() {
            Utilisateur u = utilisateurAvecRefresh(true);
            when(jwtUtil.validateRefreshToken("refresh-valide"))
                    .thenReturn(true);
            when(utilisateurRepository.findByRefreshToken("refresh-valide"))
                    .thenReturn(Optional.of(u));

            assertThatThrownBy(() ->
                    utilisateurService.refreshToken("refresh-valide"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Refresh token expiré");

            verify(jwtUtil, never()).generateToken(any());
        }

        @Test
        @DisplayName("persiste le nouveau refresh token après rotation")
        void refresh_persisteLeNouveauToken() {
            Utilisateur u = utilisateurAvecRefresh(false);
            when(jwtUtil.validateRefreshToken("refresh-valide"))
                    .thenReturn(true);
            when(utilisateurRepository.findByRefreshToken("refresh-valide"))
                    .thenReturn(Optional.of(u));
            when(jwtUtil.generateToken(any())).thenReturn("jwt");
            when(jwtUtil.generateRefreshToken()).thenReturn("tout-nouveau-refresh");
            when(utilisateurRepository.save(any())).thenReturn(u);

            utilisateurService.refreshToken("refresh-valide");

            ArgumentCaptor<Utilisateur> captor =
                    ArgumentCaptor.forClass(Utilisateur.class);
            verify(utilisateurRepository).save(captor.capture());
            assertThat(captor.getValue().getRefreshToken())
                    .isEqualTo("tout-nouveau-refresh");
        }
    }
}