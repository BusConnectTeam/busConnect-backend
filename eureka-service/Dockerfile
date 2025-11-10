# Etapa 1: Build
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# Copiamos el contenido del servicio
COPY . .

# Compilamos el proyecto (usando el wrapper si está en el repo)
RUN ./mvnw clean package -DskipTests

# Etapa 2: Runtime
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Copiamos el .jar generado
COPY --from=builder /app/target/*.jar app.jar

# Exponemos el puerto del servicio
EXPOSE 8081

# Comando de inicio
ENTRYPOINT ["java", "-jar", "app.jar"]
