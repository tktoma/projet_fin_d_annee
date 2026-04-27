package com.example.back.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_avis_reaction_utilisateur_avis",
                columnNames = {"id_utilisateur", "id_avis"})
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class AvisReaction extends BaseEntity{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_avis", nullable = false)
    private Avis avis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReactionType type;
}
