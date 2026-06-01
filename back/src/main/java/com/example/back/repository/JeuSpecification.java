package com.example.back.repository;

import com.example.back.entities.Jeu;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class JeuSpecification {

    private JeuSpecification() {}

    public static Specification<Jeu> titreLike(String titre) {
        return (root, query, cb) -> {
            if (titre == null || titre.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("titre")), "%" + titre.toLowerCase() + "%");
        };
    }

    public static Specification<Jeu> genreEgal(String genre) {
        return (root, query, cb) -> {
            if (genre == null || genre.isBlank()) return cb.conjunction();
            return cb.equal(cb.lower(root.get("genre")), genre.toLowerCase());
        };
    }

    public static Specification<Jeu> plateformeEgale(String plateforme) {
        return (root, query, cb) -> {
            if (plateforme == null || plateforme.isBlank()) return cb.conjunction();
            return cb.equal(cb.lower(root.get("plateforme")), plateforme.toLowerCase());
        };
    }

    public static Specification<Jeu> noteMoyenneMin(Float noteMin) {
        return (root, query, cb) -> {
            if (noteMin == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get("noteMoyenne"), noteMin);
        };
    }

    public static Specification<Jeu> anneeMin(Integer anneeMin) {
        return (root, query, cb) -> {
            if (anneeMin == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get("dateSortie"),
                    LocalDate.of(anneeMin, 1, 1));
        };
    }

    public static Specification<Jeu> anneeMax(Integer anneeMax) {
        return (root, query, cb) -> {
            if (anneeMax == null) return cb.conjunction();
            return cb.lessThanOrEqualTo(root.get("dateSortie"),
                    LocalDate.of(anneeMax, 12, 31));
        };
    }
}