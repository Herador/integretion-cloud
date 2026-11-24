# ---- Étape 1 : compilation ----
# Image contenant Maven et Java 21 : pas besoin d'installer Java sur ton PC.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# On télécharge d'abord les dépendances : Docker met cette étape en cache
# et ne la refait que si pom.xml change.
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Puis on compile, on lance les tests et on produit target/app.jar.
# En CI, SKIP_TESTS=true : les tests ont déjà tourné dans le job "tests".
# En local, la valeur par défaut (false) garde les tests dans le build.
COPY src ./src
ARG SKIP_TESTS=false
RUN mvn -B package -DskipTests=${SKIP_TESTS}

# ---- Étape 2 : exécution ----
# Image légère avec seulement le JRE (pas Maven, pas le code source).
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# On n'exécute pas l'application en root (bonne pratique de sécurité)
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /build/target/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
