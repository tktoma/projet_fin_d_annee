package com.example.back.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Report extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_auteur", nullable = false)
    private Utilisateur auteur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeContenu typeContenu;

    // Id de l'entité signalée (avis, note, utilisateur…)
    @Column(nullable = false)
    private Long idContenu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RaisonReport raison;

    @Column(length = 500)
    private String details;

    @Column(nullable = false)
    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutReport statut = StatutReport.EN_ATTENTE;

    // Admin qui a traité le report — null tant que non traité
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_moderateur")
    private Utilisateur moderateur;

    @Column(length = 500)
    private String noteModerateur;
}
