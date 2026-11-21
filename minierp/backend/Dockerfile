# --- Etapa 1: compilar el .jar con Maven ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Primero solo el pom.xml: Docker reutiliza las dependencias descargadas si el pom no cambia
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# --- Etapa 2: imagen liviana solo con Java para ejecutar ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

# MaxRAMPercentage ajusta la memoria de Java al límite del plan gratuito (512 MB).
# user.timezone: las fechas (LocalDateTime) y el "día de hoy" del dashboard van en hora de Colombia;
# sin esto Render usa UTC y desde las 7 p. m. las ventas caen en el día siguiente.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Duser.timezone=America/Bogota", "-jar", "app.jar"]
