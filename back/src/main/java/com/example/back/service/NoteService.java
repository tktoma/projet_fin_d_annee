package com.example.back.service;

import com.example.back.entities.Jeu;
import com.example.back.entities.Note;
import com.example.back.entities.Utilisateur;
import com.example.back.repository.JeuRepository;
import com.example.back.repository.NoteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final JeuRepository jeuRepository;

    public NoteService(NoteRepository noteRepository,
                       JeuRepository jeuRepository) {
        this.noteRepository = noteRepository;
        this.jeuRepository = jeuRepository;
    }

    // Ajouter ou modifier une note
    public Note noterJeu(Utilisateur utilisateur,
                         Long jeuId, Float valeur) {
        if (valeur < 0 || valeur > 10) {
            throw new RuntimeException(
                    "La note doit être entre 0 et 10");
        }

        Jeu jeu = jeuRepository.findById(jeuId)
                .orElseThrow(() ->
                        new RuntimeException("Jeu introuvable"));

        // Modifie si déjà noté, sinon crée
        Note note = noteRepository
                .findByUtilisateurIdAndJeuId(
                        utilisateur.getId(), jeuId)
                .orElse(new Note());

        note.setUtilisateur(utilisateur);
        note.setJeu(jeu);
        note.setValeur(valeur);
        note.setDate(LocalDate.now());
        noteRepository.save(note);

        // Met à jour la moyenne dans Jeu
        Float moyenne = noteRepository
                .calculerMoyenne(jeuId)
                .orElse(0f);
        jeu.setNoteMoyenne(moyenne);
        jeuRepository.save(jeu);

        return note;
    }

    public List<Note> getNotesDuJeu(Long jeuId) {
        return noteRepository.findByJeuId(jeuId);
    }

    public List<Note> getNotesUtilisateur(Long utilisateurId) {
        return noteRepository.findByUtilisateurId(utilisateurId);
    }

    public void supprimerNote(Utilisateur utilisateur,
                              Long jeuId) {
        Note note = noteRepository
                .findByUtilisateurIdAndJeuId(
                        utilisateur.getId(), jeuId)
                .orElseThrow(() ->
                        new RuntimeException("Note introuvable"));
        noteRepository.delete(note);
    }
}
