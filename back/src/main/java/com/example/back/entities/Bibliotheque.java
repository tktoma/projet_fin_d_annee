package com.example.back.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;


@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_bibliotheque_utilisateur_jeu",
                columnNames = {"id_utilisateur", "id_jeu"})
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString

public class Bibliotheque extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_jeu", nullable = false)
    private Jeu jeu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutJeu statut;

    @Column(nullable = false)
    private LocalDate date;
}
