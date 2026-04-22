package com.example.back.service;

import com.example.back.config.JwtUtil;
import com.example.back.dto.*;
import com.example.back.entities.Utilisateur;
import com.example.back.repository.AvisRepository;
import com.example.back.repository.BibliothequeRepository;
import com.example.back.repository.NoteRepository;
import com.example.back.repository.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AvisRepository avisRepository;
    private final NoteRepository noteRepository;
    private final BibliothequeRepository bibliothequeRepository;

    public UtilisateurService(
            UtilisateurRepository utilisateurRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            AvisRepository avisRepository,
            NoteRepository noteRepository,
            BibliothequeRepository bibliothequeRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.avisRepository = avisRepository;
        this.noteRepository = noteRepository;
        this.bibliothequeRepository = bibliothequeRepository;
    }


    // Inscription
    public AuthResponse inscrire(RegisterRequest request) {
        if (utilisateurRepository.existsByEmail(
                request.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }
        if (utilisateurRepository.existsByPseudo(
                request.getPseudo())) {
            throw new RuntimeException("Pseudo déjà utilisé");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setPseudo(request.getPseudo());
        utilisateur.setEmail(request.getEmail());
        utilisateur.setMdp(
                passwordEncoder.encode(request.getMotDePasse()));
        utilisateur.setDateCompte(LocalDate.now());

        utilisateurRepository.save(utilisateur);

        String token = jwtUtil.generateToken(utilisateur);
        String refreshToken = jwtUtil.generateRefreshToken();
        utilisateur.setRefreshToken(refreshToken);
        utilisateur.setRefreshTokenExpiration(LocalDateTime.now().plusDays(30));
        utilisateurRepository.save(utilisateur);

        return new AuthResponse(
                token,
                refreshToken,
                utilisateur.getPseudo(),
                utilisateur.getId());


    }

    // Connexion
    public AuthResponse connecter(LoginRequest request) {
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Email introuvable"));

        if (!passwordEncoder.matches(
                request.getMotDePasse(),
                utilisateur.getMdp())) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        String token = jwtUtil.generateToken(utilisateur);
        String refreshToken = jwtUtil.generateRefreshToken();
        utilisateur.setRefreshToken(refreshToken);
        utilisateur.setRefreshTokenExpiration(LocalDateTime.now().plusDays(30));
        utilisateurRepository.save(utilisateur);

        return new AuthResponse(
                token,
                refreshToken,
                utilisateur.getPseudo(),
                utilisateur.getId());
    }
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new RuntimeException("Refresh token invalide");
        }

        Utilisateur utilisateur = utilisateurRepository
                .findByRefreshToken(refreshToken)
                .orElseThrow(() ->
                        new RuntimeException("Refresh token introuvable"));

        if (utilisateur.getRefreshTokenExpiration().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expiré");
        }

        String newToken = jwtUtil.generateToken(utilisateur);
        String newRefreshToken = jwtUtil.generateRefreshToken();
        utilisateur.setRefreshToken(newRefreshToken);
        utilisateur.setRefreshTokenExpiration(LocalDateTime.now().plusDays(30));
        utilisateurRepository.save(utilisateur);

        return new AuthResponse(
                newToken,
                newRefreshToken,
                utilisateur.getPseudo(),
                utilisateur.getId());
    }
    // -------------------------------------------------------------------------
    // Profil public
    // -------------------------------------------------------------------------

    public ProfilResponse getProfilPublic(Long utilisateurId) {
        Utilisateur u = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() ->
                        new RuntimeException("Utilisateur introuvable"));

        List<AvisDto> derniers5Avis = avisRepository
                .findByUtilisateurId(utilisateurId)
                .stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .limit(5)
                .map(this::avisToDto)
                .toList();

        List<NoteDto> dernieres5Notes = noteRepository
                .findByUtilisateurId(utilisateurId)
                .stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .limit(5)
                .map(this::noteToDto)
                .toList();

        ProfilResponse profil = new ProfilResponse();
        profil.setId(u.getId());
        profil.setPseudo(u.getPseudo());
        profil.setRole(u.getRole());
        profil.setDateCompte(u.getDateCompte());
        profil.setNombreJeux(
                bibliothequeRepository
                        .findByUtilisateurId(utilisateurId).size());
        profil.setNombreAvis(
                avisRepository.findByUtilisateurId(utilisateurId).size());
        profil.setNombreNotes(
                noteRepository.findByUtilisateurId(utilisateurId).size());
        profil.setDerniersAvis(derniers5Avis);
        profil.setDernieresNotes(dernieres5Notes);

        return profil;
    }

    public List<AvisDto> getAvisPublics(Long utilisateurId) {
        verifierExistence(utilisateurId);
        return avisRepository.findByUtilisateurId(utilisateurId)
                .stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .map(this::avisToDto)
                .toList();
    }

    public List<BibliothequeDto> getBibliothequePublique(Long utilisateurId) {
        verifierExistence(utilisateurId);
        return bibliothequeRepository.findByUtilisateurId(utilisateurId)
                .stream()
                .map(this::bibliothequeToDto)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Helpers privés — conversions en DTO
    // -------------------------------------------------------------------------

    private void verifierExistence(Long utilisateurId) {
        if (!utilisateurRepository.existsById(utilisateurId)) {
            throw new RuntimeException("Utilisateur introuvable");
        }
    }

    private AvisDto avisToDto(com.example.back.entities.Avis a) {
        AvisDto dto = new AvisDto();
        dto.setId(a.getId());
        dto.setJeuId(a.getJeu().getId());
        dto.setJeuTitre(a.getJeu().getTitre());
        dto.setUtilisateurId(a.getUtilisateur().getId());
        dto.setUtilisateurPseudo(a.getUtilisateur().getPseudo());
        dto.setTexte(a.getTexte());
        dto.setLikes(a.getLikes());
        dto.setDislikes(a.getDislikes());
        dto.setDate(a.getDate());
        return dto;
    }

    private NoteDto noteToDto(com.example.back.entities.Note n) {
        NoteDto dto = new NoteDto();
        dto.setId(n.getId());
        dto.setJeuId(n.getJeu().getId());
        dto.setJeuTitre(n.getJeu().getTitre());
        dto.setUtilisateurId(n.getUtilisateur().getId());
        dto.setUtilisateurPseudo(n.getUtilisateur().getPseudo());
        dto.setValeur(n.getValeur());
        dto.setDate(n.getDate());
        return dto;
    }

    private BibliothequeDto bibliothequeToDto(
            com.example.back.entities.Bibliotheque b) {
        BibliothequeDto dto = new BibliothequeDto();
        dto.setId(b.getId());
        dto.setJeuId(b.getJeu().getId());
        dto.setJeuTitre(b.getJeu().getTitre());
        dto.setJeuCoverUrl(b.getJeu().getCoverUrl());
        dto.setStatut(b.getStatut());
        dto.setDate(b.getDate());
        return dto;
    }
}
