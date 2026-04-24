package com.example.back.service;

import com.example.back.config.JwtUtil;
import com.example.back.dto.*;
import com.example.back.entities.Utilisateur;
import com.example.back.exception.ConflictException;
import com.example.back.exception.NotFoundException;
import com.example.back.exception.TokenExpiredException;
import com.example.back.repository.AvisRepository;
import com.example.back.repository.BibliothequeRepository;
import com.example.back.repository.NoteRepository;
import com.example.back.repository.UtilisateurRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    private AuthResponse createAuthResponse(Utilisateur utilisateur) {
        String token = jwtUtil.generateToken(utilisateur);
        String refreshToken = jwtUtil.generateRefreshToken();
        utilisateur.setRefreshToken(refreshToken);
        utilisateur.setRefreshTokenExpiration(
                LocalDateTime.now().plusDays(30));
        utilisateurRepository.save(utilisateur);

        return new AuthResponse(token, refreshToken,
                utilisateur.getPseudo(), utilisateur.getId());
    }

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
    @Transactional
    public AuthResponse inscrire(RegisterRequest request) {
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email déjà utilisé");
        }
        if (utilisateurRepository.existsByPseudo(request.getPseudo())) {
            throw new ConflictException("Pseudo déjà utilisé");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setPseudo(request.getPseudo());
        utilisateur.setEmail(request.getEmail());
        utilisateur.setMdp(passwordEncoder.encode(request.getMotDePasse()));
        utilisateur.setDateCompte(LocalDate.now());
        utilisateurRepository.save(utilisateur);

        return createAuthResponse(utilisateur);
    }

    // Connexion
    @Transactional
    public AuthResponse connecter(LoginRequest request) {
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new NotFoundException("Email introuvable"));

        if (!passwordEncoder.matches(
                request.getMotDePasse(), utilisateur.getMdp())) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        return createAuthResponse(utilisateur);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new RuntimeException("Refresh token invalide");
        }

        Utilisateur utilisateur = utilisateurRepository
                .findByRefreshToken(refreshToken)
                .orElseThrow(() ->
                        new NotFoundException("Refresh token introuvable"));

        if (utilisateur.getRefreshTokenExpiration()
                .isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException("Refresh token expiré");
        }

        return createAuthResponse(utilisateur);
    }
    // -------------------------------------------------------------------------
    // Profil public
    // -------------------------------------------------------------------------

    public ProfilResponse getProfilPublic(Long utilisateurId) {
        Utilisateur u = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));

        Pageable pageable = PageRequest.of(0, 5);

        List<AvisDto> derniers5Avis = avisRepository
                .findByUtilisateurIdOrderByDateDesc(utilisateurId, pageable)
                .getContent()
                .stream()
                .map(ResponseMapper::toAvisDto)
                .toList();

        List<NoteDto> dernieres5Notes = noteRepository
                .findByUtilisateurIdOrderByDateDesc(utilisateurId, pageable)
                .getContent()
                .stream()
                .map(ResponseMapper::toNoteDto)
                .toList();

        ProfilResponse profil = new ProfilResponse();
        profil.setId(u.getId());
        profil.setPseudo(u.getPseudo());
        profil.setRole(u.getRole());
        profil.setDateCompte(u.getDateCompte());
        profil.setNombreJeux(
                (int) bibliothequeRepository.countByUtilisateurId(utilisateurId));
        profil.setNombreAvis(
                (int) avisRepository.countByUtilisateurId(utilisateurId));
        profil.setNombreNotes(
                (int) noteRepository.countByUtilisateurId(utilisateurId));
        profil.setDerniersAvis(derniers5Avis);      // ✅ assignation manquante
        profil.setDernieresNotes(dernieres5Notes);  // ✅ assignation manquante

        return profil;
    }

    public List<AvisDto> getAvisPublics(Long utilisateurId) {
        verifierExistence(utilisateurId);
        return avisRepository.findByUtilisateurId(utilisateurId)
                .stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .map(ResponseMapper::toAvisDto)
                .toList();
    }

    public List<BibliothequeDto> getBibliothequePublique(Long utilisateurId) {
        verifierExistence(utilisateurId);
        return bibliothequeRepository.findByUtilisateurId(utilisateurId)
                .stream()
                .map(ResponseMapper::toBibliothequeDto)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Helpers privés — conversions en DTO
    // -------------------------------------------------------------------------

    private void verifierExistence(Long utilisateurId) {
        if (!utilisateurRepository.existsById(utilisateurId)) {
            throw new NotFoundException("Utilisateur introuvable");
        }
    }
}
