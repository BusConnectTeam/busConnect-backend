# ============================================
# Stage 1: Build
# ============================================
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copiar todo el proyecto (ahora que el contexto es la raíz)
COPY . .

# Compilar solo el módulo catalog-service (y sus dependencias del padre)
RUN mvn -pl catalog-service -am clean package -DskipTests

# ============================================
# Stage 2: Runtime
# ============================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Crear usuario no-root por seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar JAR generado desde el stage anterior
COPY --from=build /app/catalog-service/target/*.jar app.jar

# Exponer puerto
EXPOSE 8083

# Variables de entorno con valores por defecto
ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Healthcheck específico para WebFlux
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

# Ejecutar aplicación
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]