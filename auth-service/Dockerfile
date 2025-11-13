# ==============================================================================
# 1️⃣ ETAPA DE CONSTRUCCIÓN (BUILD STAGE)
# Compila el proyecto usando Maven y Java 21.
# ==============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

# Crea y define el directorio de trabajo
WORKDIR /app

# Copia solo los archivos necesarios para descargar dependencias primero (mejor cache)
COPY pom.xml .
COPY ../pom.xml ../pom.xml
COPY ../.mvn ../.mvn
COPY ../mvnw ../mvnw

# Descarga las dependencias (sin compilar todavía)
RUN chmod +x ../mvnw && ../mvnw dependency:go-offline -B

# Copia el código fuente del microservicio
COPY src ./src

# Compila y empaqueta el microservicio (sin ejecutar tests)
RUN ../mvnw clean package -DskipTests

# ==============================================================================
# 2️⃣ ETAPA DE EJECUCIÓN (RUNTIME STAGE)
# Imagen ligera solo con el JRE para ejecutar la app.
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine

# Puerto que expone el microservicio (8081 para auth-service)
EXPOSE 8081

# Por seguridad, se ejecuta como usuario no root
USER 1000

# Crea directorio de trabajo
WORKDIR /app

# Copia el JAR desde la etapa anterior
COPY --from=builder /app/target/*.jar app.jar

# Comando de inicio
ENTRYPOINT ["java", "-jar", "app.jar"]
