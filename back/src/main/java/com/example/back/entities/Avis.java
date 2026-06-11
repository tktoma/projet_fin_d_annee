package com.example.back.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class Avis extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_jeu", nullable = false)
    private Jeu jeu;

    @Column(nullable = false, length = 2000)
    private String texte;

    @Builder.Default
    @Column
    private Integer likes = 0;

    @Builder.Default
    @Column
    private Integer dislikes = 0;

    @Column(nullable = false)
    private LocalDate date;

    // Supprime automatiquement les réactions quand l'avis est supprimé
    @OneToMany(mappedBy = "avis",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<AvisReaction> reactions = new ArrayList<>();
}