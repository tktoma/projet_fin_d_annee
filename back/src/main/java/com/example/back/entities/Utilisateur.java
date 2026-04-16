package com.example.back.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString

public class Utilisateur extends BaseEntity{

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
    private Role role = Role.USER; // USER par défaut

    // Dans la classe Utilisateur, après le champ role :
    @Column(unique = true)
    private String refreshToken;

    @Column
    private LocalDateTime refreshTokenExpiration;

}
