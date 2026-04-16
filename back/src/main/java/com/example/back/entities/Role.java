package com.example.back.entities;

public enum Role {
    USER,       // utilisateur de base
    POSTER,     // peut ajouter des jeux
    ADMIN,      // modération + gestion users
    SUPERADMIN  // tous les droits
}
