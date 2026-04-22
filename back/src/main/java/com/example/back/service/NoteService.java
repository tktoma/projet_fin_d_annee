package com.example.back.service;

import com.example.back.dto.NoteDto;
import com.example.back.dto.ResponseMapper;
import com.example.back.entities.Jeu;
import com.example.back.entities.Note;
import com.example.back.entities.Utilisateur;
import com.example.back.exception.NotFoundException;
import com.example.back.repository.JeuRepository;
import com.example.back.repository.NoteRepository;
import jakarta.transaction.Transactional;
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
    @Transactional
    public NoteDto noterJeu(Utilisateur utilisateur,
                            Long jeuId, Float valeur) {
        if (valeur < 0 || valeur > 10) {
            throw new RuntimeException(
                    "La note doit être entre 0 et 10");
        }

        Jeu jeu = jeuRepository.findById(jeuId)
                .orElseThrow(() ->
                        new NotFoundException("Jeu introuvable"));


        // Modifie si déjà noté, sinon crée
        Note note = noteRepository
                .findByUtilisateurIdAndJeuId(
                        utilisateur.getId(), jeuId)
                .orElseGet(Note::new);

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

        return ResponseMapper.toNoteDto(note);
    }

    public List<NoteDto> getNotesDuJeu(Long jeuId) {
        return noteRepository.findByJeuId(jeuId)
                .stream()
                .map(ResponseMapper::toNoteDto)
                .toList();
    }

    public List<NoteDto> getNotesUtilisateur(Long utilisateurId) {
        return noteRepository.findByUtilisateurId(utilisateurId)
                .stream()
                .map(ResponseMapper::toNoteDto)
                .toList();
    }

    public void supprimerNote(Utilisateur utilisateur,
                              Long jeuId) {
        Note note = noteRepository
                .findByUtilisateurIdAndJeuId(
                        utilisateur.getId(), jeuId)
                .orElseThrow(() ->
                        new NotFoundException("Note introuvable"));
        noteRepository.delete(note);
    }
}
