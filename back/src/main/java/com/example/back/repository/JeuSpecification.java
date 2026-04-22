package com.example.back.repository;

import com.example.back.entities.Jeu;
import org.springframework.data.jpa.domain.Specification;

public class JeuSpecification {

    private JeuSpecification() {}

    /**
     * Filtre sur le titre — insensible à la casse, recherche partielle.
     * Ex : "zelda" matche "The Legend of Zelda".
     */
    public static Specification<Jeu> titreLike(String titre) {
        if (titre == null || titre.isBlank()) return null;
        return (root, query, cb) ->
                cb.like(
                        cb.lower(root.get("titre")),
                        "%" + titre.toLowerCase() + "%"
                );
    }

    /**
     * Filtre sur le genre — insensible à la casse, correspondance exacte.
     * Ex : "RPG" matche "RPG" mais pas "Action-RPG".
     */
    public static Specification<Jeu> genreEgal(String genre) {
        if (genre == null || genre.isBlank()) return null;
        return (root, query, cb) ->
                cb.equal(
                        cb.lower(root.get("genre")),
                        genre.toLowerCase()
                );
    }

    /**
     * Filtre sur la plateforme — insensible à la casse, correspondance exacte.
     * Ex : "PC" matche "PC".
     */
    public static Specification<Jeu> plateformeEgale(String plateforme) {
        if (plateforme == null || plateforme.isBlank()) return null;
        return (root, query, cb) ->
                cb.equal(
                        cb.lower(root.get("plateforme")),
                        plateforme.toLowerCase()
                );
    }

    /**
     * Filtre sur la note moyenne — retourne les jeux avec noteMoyenne >= min.
     * Ex : 7.5 retourne tous les jeux notés 7.5 ou plus.
     */
    public static Specification<Jeu> noteMoyenneMin(Float noteMin) {
        if (noteMin == null) return null;
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(
                        root.get("noteMoyenne"),
                        noteMin
                );
    }
}