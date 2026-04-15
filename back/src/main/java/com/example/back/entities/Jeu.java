package com.example.back.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;
import java.time.LocalDate;


@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString

public class Jeu extends BaseEntity{

    @Column(nullable = false)
    private  String titre;
    @Column
    private  String genre;
    @Column
    private  String plateforme;
    @Column
    private String coverUrl;
    @Column
    private LocalDate dateSortie;
    @Column
    private String source;
    @Column(nullable = false, precision = 2, length = 2)
    private float noteMoyenne;
    @Column
    private String source_IGBD;         // ex: "igdb"
    @Column
    private String externalId;     // id dans la BDD IGDB
}
