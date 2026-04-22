package com.example.back.service;

import com.example.back.dto.AvatarDto;
import com.example.back.dto.ResponseMapper;
import com.example.back.entities.Avatar;
import com.example.back.entities.Utilisateur;
import com.example.back.repository.AvatarRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AvatarService {

    private static final long TAILLE_MAX = 2 * 1024 * 1024; // 2 Mo
    private static final List<String> TYPES_AUTORISES =
            List.of("image/jpeg", "image/png", "image/webp");

    @Value("${avatar.upload-dir:uploads/avatars}")
    private String uploadDir;

    @Value("${avatar.base-url:http://localhost:8080/avatars}")
    private String baseUrl;

    private final AvatarRepository avatarRepository;

    public AvatarService(AvatarRepository avatarRepository) {
        this.avatarRepository = avatarRepository;
    }

    public AvatarDto uploadAvatar(Utilisateur utilisateur,
                                  MultipartFile fichier)
            throws IOException {

        validerFichier(fichier);

        // Créer le dossier si nécessaire
        Path dossier = Paths.get(uploadDir);
        Files.createDirectories(dossier);

        // Supprimer l'ancien avatar si existant
        avatarRepository.findByUtilisateurId(utilisateur.getId())
                .ifPresent(ancien -> {
                    supprimerFichier(ancien.getUrl());
                    avatarRepository.delete(ancien);
                });

        // Générer un nom de fichier unique
        String extension = getExtension(
                fichier.getOriginalFilename());
        String nomFichier = UUID.randomUUID() + extension;
        Path chemin = dossier.resolve(nomFichier);

        fichier.transferTo(chemin);

        Avatar avatar = new Avatar();
        avatar.setUtilisateur(utilisateur);
        avatar.setUrl(baseUrl + "/" + nomFichier);
        avatar.setContentType(fichier.getContentType());
        avatar.setTaille(fichier.getSize());

        return ResponseMapper.toAvatarDto(
                avatarRepository.save(avatar));
    }

    public Optional<AvatarDto> getAvatar(Long utilisateurId) {
        return avatarRepository
                .findByUtilisateurId(utilisateurId)
                .map(ResponseMapper::toAvatarDto);
    }

    public void supprimerAvatar(Utilisateur utilisateur) {
        Avatar avatar = avatarRepository
                .findByUtilisateurId(utilisateur.getId())
                .orElseThrow(() ->
                        new RuntimeException("Avatar introuvable"));
        supprimerFichier(avatar.getUrl());
        avatarRepository.delete(avatar);
    }

    // -------------------------------------------------------------------------
    // Helpers privés
    // -------------------------------------------------------------------------

    private void validerFichier(MultipartFile fichier) {
        if (fichier.isEmpty()) {
            throw new RuntimeException("Le fichier est vide");
        }
        if (fichier.getSize() > TAILLE_MAX) {
            throw new RuntimeException(
                    "Fichier trop volumineux (max 2 Mo)");
        }
        if (!TYPES_AUTORISES.contains(fichier.getContentType())) {
            throw new RuntimeException(
                    "Format non autorisé (JPEG, PNG ou WebP uniquement)");
        }
    }

    private void supprimerFichier(String url) {
        try {
            String nomFichier = url.substring(
                    url.lastIndexOf("/") + 1);
            Path chemin = Paths.get(uploadDir, nomFichier);
            Files.deleteIfExists(chemin);
        } catch (IOException e) {
            System.err.println("Impossible de supprimer "
                    + "l'ancien avatar : " + e.getMessage());
        }
    }

    private String getExtension(String nomFichier) {
        if (nomFichier == null || !nomFichier.contains(".")) {
            return ".jpg";
        }
        return nomFichier.substring(
                nomFichier.lastIndexOf("."));
    }
}