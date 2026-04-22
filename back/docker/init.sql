
-- Recréer avec @'%' — accepte les connexions depuis n'importe quel hôte
-- Accordé automatiquement par MARIADB_USER/MARIADB_PASSWORD mais
-- on s'assure que l'utilisateur a tous les droits sur la base
CREATE USER IF NOT EXISTS 'back_user'@'%'
    IDENTIFIED BY 'back_password';

-- Donner tous les droits sur la base
GRANT ALL PRIVILEGES ON back_db.* TO 'back_user'@'%';