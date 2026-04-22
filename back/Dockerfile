# ─── Stage 1 : build ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copier uniquement les fichiers Maven en premier pour profiter du cache
# des layers Docker — les dépendances ne sont re-téléchargées que si
# pom.xml change, pas à chaque modification du code source
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Télécharger toutes les dépendances sans compiler le code
RUN ./mvnw dependency:go-offline -B

# Copier le code source et compiler
COPY src src
RUN ./mvnw package -DskipTests -B

# ─── Stage 2 : runtime ──────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Utilisateur non-root — bonne pratique de sécurité
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Copier uniquement le jar produit par le stage build
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]