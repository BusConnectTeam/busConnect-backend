# ============================================
# Stage 1: Build
# ============================================
# Usamos Maven + JDK ya preparado para build
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY . .

# Descargar dependencias y compilar solo auth-service
RUN mvn -pl auth-service -am clean package -DskipTests

# ============================================
# Stage 2: Runtime
# ============================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app


# Crear usuario no-root (opcional, más seguro)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar JAR generado desde la etapa de build
COPY --from=build /app/auth-service/target/*.jar app.jar

# Exponer puerto
EXPOSE 8081

# Variables de entorno opcionales
ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Healthcheck opcional
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Ejecutar la aplicación
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
