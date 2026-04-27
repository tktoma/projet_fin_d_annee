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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AvatarService {

    private static final long TAILLE_MAX = 2 * 1024 * 1024; // 2 Mo
    private static final List<String> TYPES_AUTORISES =
            List.of("image/jpeg", "image/png", "image/webp");

    /**
     * Mapping contentType → extension.
     * On dérive l'extension du type MIME réel plutôt que du nom de fichier
     * pour éviter la discordance (ex : fichier.jpg uploadé en PNG).
     */
    private static final Map<String, String> EXTENSION_PAR_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png",  ".png",
            "image/webp", ".webp"
    );

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

        Path dossier = Paths.get(uploadDir);
        Files.createDirectories(dossier);

        avatarRepository.findByUtilisateurId(utilisateur.getId())
                .ifPresent(ancien -> {
                    supprimerFichier(ancien.getUrl());
                    avatarRepository.delete(ancien);
                });

        // Extension dérivée du contentType — pas du nom de fichier
        String extension = EXTENSION_PAR_TYPE
                .getOrDefault(fichier.getContentType(), ".jpg");
        String nomFichier = UUID.randomUUID() + extension;
        Path chemin = dossier.resolve(nomFichier);

        fichier.transferTo(chemin);

        Avatar avatar = new Avatar();
        avatar.setUtilisateur(utilisateur);
        avatar.setUrl(normaliserUrl(baseUrl) + "/" + nomFichier);
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
            String nomFichier = url.substring(url.lastIndexOf("/") + 1);
            Path chemin = Paths.get(uploadDir, nomFichier);
            Files.deleteIfExists(chemin);
        } catch (IOException e) {
            System.err.println("Impossible de supprimer "
                    + "l'ancien avatar : " + e.getMessage());
        }
    }

    private String normaliserUrl(String url) {
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}