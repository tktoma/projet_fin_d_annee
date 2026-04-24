package com.example.back.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Utilisateur extends BaseEntity {

    @Column(nullable = false, length = 20, unique = true)
    private String pseudo;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String mdp;

    @Column(nullable = false)
    private LocalDate dateCompte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(unique = true)
    private String refreshToken;

    @Column
    private LocalDateTime refreshTokenExpiration;

    // Cascade DELETE — supprime automatiquement les entités liées
    // quand l'utilisateur est supprimé
    @OneToMany(mappedBy = "utilisateur",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Avis> avis = new ArrayList<>();

    @OneToMany(mappedBy = "utilisateur",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Note> notes = new ArrayList<>();

    @OneToMany(mappedBy = "utilisateur",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Bibliotheque> bibliotheque = new ArrayList<>();

    @OneToOne(mappedBy = "utilisateur",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @ToString.Exclude
    private Avatar avatar;
}