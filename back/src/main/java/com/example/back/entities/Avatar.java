package com.example.back.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Avatar extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur",
            nullable = false, unique = true)
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false, length = 50)
    private String contentType;

    @Column(nullable = false)
    private Long taille;
}