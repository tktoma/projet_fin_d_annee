package com.example.back.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString

public class Avis extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_jeu", nullable = false)
    private Jeu jeu;

    @Column(nullable = false, length = 2000)
    private String texte;

    @Column
    private Integer likes = 0;

    @Column
    private Integer dislikes = 0;

    @Column(nullable = false)
    private LocalDate date;
}
