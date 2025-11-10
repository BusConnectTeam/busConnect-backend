# Etapa 1: build con Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos pom.xml y resolvemos dependencias (cache)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiamos el código fuente
COPY src ./src

# Compilamos el proyecto (salida jar en /target)
RUN mvn clean package -DskipTests

# Etapa 2: imagen final de ejecución
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copiamos el jar generado desde la etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Exponemos el puerto (debe coincidir con application.yml)
EXPOSE 8081

# Comando de inicio
ENTRYPOINT ["java", "-jar", "app.jar"]