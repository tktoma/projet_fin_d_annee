package com.example.back.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_note_utilisateur_jeu",
                columnNames = {"id_utilisateur", "id_jeu"})
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString

public class Note extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_jeu", nullable = false)
    private Jeu jeu;

    @Column(nullable = false, length = 2)
    private float valeur;

    @Column(name = "date_note")
    private LocalDate date;
}
