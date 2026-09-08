# Construction : le JDK compile et emballe le jar. Les tests tournent dans la CI, pas ici.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Les dépendances d'abord : cette couche reste en cache tant que le pom ne change pas
COPY .mvn/ .mvn/
COPY mvnw pom.xml checkstyle.xml ./
RUN ./mvnw -B -ntp dependency:go-offline

COPY src/main/ src/main/
RUN ./mvnw -B -ntp package -Dmaven.test.skip=true

# Exécution : un JRE seul, sous un utilisateur sans privilège
FROM eclipse-temurin:21-jre
RUN useradd --system --no-create-home pms
WORKDIR /app
COPY --from=build /app/target/hotel-0.0.1-SNAPSHOT.jar app.jar
USER pms
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
