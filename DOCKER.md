# 🐳 Docker Setup - BusConnect Backend

Guía completa para configurar y usar Docker en el proyecto BusConnect Backend.

---

## 📋 Requisitos previos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado y corriendo
- [Docker Compose](https://docs.docker.com/compose/install/) (incluido en Docker Desktop)
- Git

**Versiones recomendadas:**
- Docker: 24.0+
- Docker Compose: 2.20+

---

## 🚀 Quick Start (5 minutos)

### 1. Clonar el repositorio

```bash
git clone https://github.com/BusConnectTeam/busConnect-backend.git
cd busConnect-backend
```

### 2. Configurar variables de entorno

```bash
# Copiar plantilla
cp .env.example .env

# Editar con tu contraseña (usa tu editor favorito)
nano .env
# o
code .env
```

**Edita al menos esta variable:**
```properties
POSTGRES_PASSWORD=tu_contraseña_segura_aqui
```

### 3. Crear esquemas de base de datos

**IMPORTANTE:** Los esquemas deben crearse manualmente la primera vez.

```bash
# Levantar solo PostgreSQL
docker-compose up -d postgres

# Esperar unos segundos a que inicie
sleep 5

# Conectar a PostgreSQL y crear esquema
docker exec -it busconnect-postgres psql -U busconnect_user -d busconnectdb
```

Dentro de psql, ejecuta:
```sql
-- Crear esquema para user-service
CREATE SCHEMA user_service AUTHORIZATION busconnect_user;

-- Verificar que se creó
\dn

-- Salir
\q
```

### 4. Levantar todos los servicios

```bash
docker-compose up -d
```

### 5. Verificar que todo funciona

**Servicios disponibles:**
- 🗄️ **PostgreSQL:** `localhost:5432`
- 🔧 **pgAdmin:** http://localhost:5050
    - Email: `admin@busconnect.com`
    - Password: `admin123` (o el que configuraste)
- 🚀 **user-service:**
    - API: http://localhost:8082
    - Swagger: http://localhost:8082/swagger-ui.html
    - Health: http://localhost:8082/actuator/health

**Ver logs:**
```bash
docker-compose logs -f
```

---

## 📚 Comandos útiles

### Gestión de servicios

```bash
# Ver estado de todos los servicios
docker-compose ps

# Levantar todos los servicios
docker-compose up -d

# Levantar un servicio específico
docker-compose up -d user-service

# Ver logs de todos los servicios
docker-compose logs -f

# Ver logs de un servicio específico
docker-compose logs -f user-service

# Parar todos los servicios
docker-compose down

# Parar y eliminar volúmenes (¡CUIDADO! Borra datos de BD)
docker-compose down -v

# Reiniciar un servicio
docker-compose restart user-service

# Reconstruir después de cambios en el código
docker-compose up --build user-service

# Reconstruir sin usar caché
docker-compose build --no-cache user-service
```

### Gestión de base de datos

```bash
# Conectar a PostgreSQL desde terminal
docker exec -it busconnect-postgres psql -U busconnect_user -d busconnectdb

# Ejecutar un SQL desde archivo
docker exec -i busconnect-postgres psql -U busconnect_user -d busconnectdb < script.sql

# Backup de la base de datos
docker exec busconnect-postgres pg_dump -U busconnect_user busconnectdb > backup.sql

# Restaurar backup
docker exec -i busconnect-postgres psql -U busconnect_user -d busconnectdb < backup.sql

# Ver esquemas existentes
docker exec -it busconnect-postgres psql -U busconnect_user -d busconnectdb -c "\dn"
```

### Limpieza y mantenimiento

```bash
# Ver espacio usado por Docker
docker system df

# Limpiar recursos no usados
docker system prune

# Limpiar imágenes antiguas
docker image prune -a

# Ver volúmenes
docker volume ls

# Eliminar volumen específico (¡CUIDADO! Borra datos)
docker volume rm busconnect-backend_postgres-data
```

---

## 🏗️ Arquitectura Docker

### Servicios definidos

```yaml
servicios:
  postgres:       # Base de datos compartida
    - Puerto: 5432
    - Volumen: postgres-data (persistente)
    - Healthcheck: Verifica disponibilidad
    
  pgadmin:        # Herramienta de gestión visual
    - Puerto: 5050
    - Volumen: pgadmin-data (persistente)
    - Depende de: postgres
    
  user-service:   # Microservicio de usuarios
    - Puerto: 8082
    - Build: Dockerfile multi-stage
    - Depende de: postgres (con healthcheck)
    - Perfil: docker (usa application-docker.yml)
```

### Red compartida

Todos los servicios están en la red `busconnect-network` (bridge), permitiendo comunicación entre contenedores usando nombres de servicio:

```yaml
user-service puede conectarse a postgres:5432
pgadmin puede conectarse a postgres:5432
```

---

## 👥 Añadir un nuevo microservicio

### Para desarrolladoras del equipo:

#### 1. Crear esquema en PostgreSQL

```bash
docker exec -it busconnect-postgres psql -U busconnect_user -d busconnectdb
```

```sql
CREATE SCHEMA auth_service AUTHORIZATION busconnect_user;
\q
```

#### 2. Crear Dockerfile en tu servicio

Crea `auth-service/Dockerfile` (usa `user-service/Dockerfile` como referencia):

```dockerfile
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY ../pom.xml ../pom.xml
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 3. Añadir servicio al docker-compose.yml

```yaml
  auth-service:
    build:
      context: ./auth-service
      dockerfile: Dockerfile
    container_name: busconnect-auth-service
    restart: unless-stopped
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: ${POSTGRES_DB:-busconnectdb}
      DB_USERNAME: ${POSTGRES_USER:-busconnect_user}
      DB_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "8081:8081"
    networks:
      - busconnect-network
    depends_on:
      postgres:
        condition: service_healthy
```

#### 4. Crear .dockerignore

Crea `auth-service/.dockerignore` (copia de `user-service/.dockerignore`)

#### 5. Configurar application.yml

Asegúrate de que tu `application.yml` tenga:

```yaml
# Perfil para Docker
spring:
  config:
    activate:
      on-profile: docker
  
  datasource:
    url: jdbc:postgresql://postgres:5432/${DB_NAME}
  
  jpa:
    properties:
      hibernate:
        default_schema: auth_service  # Tu esquema específico
```

#### 6. Probar localmente

```bash
# Reconstruir y levantar
docker-compose up --build auth-service

# Ver logs
docker-compose logs -f auth-service
```

#### 7. Hacer PR

Antes de hacer PR:
- Hacer pull de develop (puede haber conflictos en docker-compose.yml)
- Resolver conflictos manualmente
- Probar que todo funciona
- Commit y push

---

## 🐛 Troubleshooting

### ❌ Error: "password authentication failed"

**Causa:** Contraseña incorrecta en `.env`

**Solución:**
```bash
# Verificar .env
cat .env | grep POSTGRES_PASSWORD

# Si es incorrecta, editar
nano .env

# Reiniciar servicios
docker-compose down
docker-compose up -d
```

---

### ❌ Error: "schema does not exist"

**Causa:** No se creó el esquema en PostgreSQL

**Solución:**
```bash
# Conectar a PostgreSQL
docker exec -it busconnect-postgres psql -U busconnect_user -d busconnectdb

# Crear esquema
CREATE SCHEMA user_service AUTHORIZATION busconnect_user;
\q
```

---

### ❌ Error: "port already in use"

**Causa:** Ya tienes PostgreSQL u otro servicio corriendo en ese puerto

**Solución Opción 1 (parar el otro servicio):**
```bash
# Windows
services.msc  # Buscar PostgreSQL y detenerlo

# Mac
brew services stop postgresql

# Linux
sudo systemctl stop postgresql
```

**Solución Opción 2 (cambiar puerto en Docker):**

Edita `docker-compose.yml`:
```yaml
postgres:
  ports:
    - "5433:5432"  # Puerto externo diferente
```

---

### ❌ Error: "Cannot connect to Docker daemon"

**Causa:** Docker Desktop no está corriendo

**Solución:**
- Abre Docker Desktop
- Espera a que inicie completamente (ícono verde)
- Intenta de nuevo

---

### ❌ El servicio no arranca

**Ver logs detallados:**
```bash
docker-compose logs user-service
```

**Reconstruir desde cero:**
```bash
# Parar todo
docker-compose down

# Limpiar volúmenes (¡CUIDADO! Borra datos)
docker-compose down -v

# Reconstruir sin caché
docker-compose build --no-cache

# Levantar
docker-compose up -d

# Ver logs
docker-compose logs -f
```

---

### ❌ "No space left on device"

**Causa:** Docker está usando mucho espacio

**Solución:**
```bash
# Ver espacio usado
docker system df

# Limpiar recursos no usados
docker system prune -a

# Eliminar volúmenes no usados
docker volume prune
```

---

### ❌ Cambios en código no se reflejan

**Causa:** La imagen Docker no se reconstruyó

**Solución:**
```bash
# Reconstruir forzando
docker-compose up --build user-service

# O reconstruir sin caché
docker-compose build --no-cache user-service
docker-compose up -d user-service
```

---

### 🔄 Reiniciar desde cero (ÚLTIMA OPCIÓN)

**⚠️ CUIDADO: Esto elimina TODOS los datos**

```bash
# 1. Parar todo
docker-compose down -v

# 2. Eliminar volúmenes
docker volume rm busconnect-backend_postgres-data
docker volume rm busconnect-backend_pgadmin-data

# 3. Limpiar sistema Docker
docker system prune -a

# 4. Volver a hacer setup completo
docker-compose up -d postgres
# Crear esquemas...
docker-compose up -d
```

---

## 🔒 Seguridad

### Variables de entorno

**NUNCA subas a Git:**
- ❌ `.env` (con contraseñas reales)
- ❌ `.env.local`
- ❌ Cualquier archivo con secrets

**SÍ puedes subir:**
- ✅ `.env.example` (plantilla sin secrets)
- ✅ `docker-compose.yml` (usa variables)
- ✅ Documentación

### Contraseñas

**Para desarrollo:**
- Usa contraseñas locales (no las reales de producción)
- Cada desarrolladora tiene sus propias contraseñas

**Para producción:**
- Usa variables de entorno del servidor
- Usa servicios de gestión de secrets (AWS Secrets Manager, etc.)
- NUNCA reutilices contraseñas de desarrollo

---

## 📖 Recursos adicionales

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot with Docker](https://spring.io/guides/topicals/spring-boot-docker/)
- [PostgreSQL Docker Hub](https://hub.docker.com/_/postgres)

---

## 💡 Tips y mejores prácticas

### Desarrollo eficiente

```bash
# Ver logs solo de errores
docker-compose logs user-service | grep ERROR

# Ejecutar comandos dentro del contenedor
docker exec -it busconnect-user-service bash

# Ver variables de entorno del contenedor
docker exec busconnect-user-service env

# Copiar archivos desde/hacia contenedor
docker cp busconnect-user-service:/app/logs/app.log ./local.log
```

### Optimización

- Usa `.dockerignore` para reducir tamaño del contexto de build
- Aprovecha el caché de capas de Docker
- Multi-stage builds reducen tamaño final de imagen
- Limpia recursos periódicamente con `docker system prune`

### Debugging

```bash
# Entrar al contenedor de user-service
docker exec -it busconnect-user-service bash

# Ver estructura de archivos
ls -la

# Ver logs de la aplicación (si usas logging)
cat logs/application.log

# Verificar conectividad a postgres
nc -zv postgres 5432
```

---

## ❓ Preguntas frecuentes

### ¿Por qué usar Docker?

- ✅ Entorno consistente en todos los equipos
- ✅ No "en mi máquina funciona"
- ✅ Setup rápido para nuevos miembros
- ✅ Aislamiento de servicios
- ✅ Fácil de escalar a producción

### ¿Puedo trabajar sin Docker?

Sí, pero **no es recomendado**. Ver sección de [Setup sin Docker](#-alternativa-setup-sin-docker) al final.

### ¿Los datos persisten al reiniciar?

**Sí**, gracias a los volúmenes:
- `postgres-data`: Datos de PostgreSQL
- `pgadmin-data`: Configuración de pgAdmin

**EXCEPTO** si usas `docker-compose down -v` (elimina volúmenes).

### ¿Cómo actualizo la versión de PostgreSQL?

Edita `docker-compose.yml`:
```yaml
postgres:
  image: postgres:17-alpine  # Cambiar versión
```

Luego:
```bash
docker-compose down
docker-compose up -d postgres
```

---

## 🛠️ Alternativa: Setup sin Docker

<details>
<summary><strong>⚠️ Solo si tienes problemas con Docker</strong> - Click para expandir</summary>

### Requisitos
- Java 21
- Maven 3.9+
- PostgreSQL 16

### 1. Instalar PostgreSQL

**Windows:** [Descargar instalador](https://www.postgresql.org/download/windows/)  
**Mac:** `brew install postgresql@16`  
**Linux:** `sudo apt install postgresql-16`

### 2. Configurar base de datos

```sql
-- Conectar como superusuario
psql -U postgres

-- Crear base de datos
CREATE DATABASE busconnectdb;

-- Crear usuario
CREATE USER busconnect_user WITH PASSWORD 'tu_password';

-- Dar permisos
GRANT ALL PRIVILEGES ON DATABASE busconnectdb TO busconnect_user;
ALTER DATABASE busconnectdb OWNER TO busconnect_user;

-- Cambiar a la BD
\c busconnectdb

-- Crear esquema
CREATE SCHEMA user_service AUTHORIZATION busconnect_user;

-- Salir
\q
```

### 3. Configurar variables de entorno

```bash
cd user-service
cp .env.example .env.local
nano .env.local
```

Editar:
```properties
DB_HOST=localhost
DB_PORT=5432
DB_NAME=busconnectdb
DB_USERNAME=busconnect_user
DB_PASSWORD=tu_password_real
```

### 4. Compilar y ejecutar

```bash
# En la raíz del proyecto
mvn clean install

# Ejecutar user-service
cd user-service
mvn spring-boot:run
```

### 5. Verificar

- API: http://localhost:8082
- Swagger: http://localhost:8082/swagger-ui.html

</details>

---

## 📞 Soporte

Si tienes problemas:
1. Revisa la sección [Troubleshooting](#-troubleshooting)
2. Busca en los logs: `docker-compose logs -f`
3. Pregunta en el canal de equipo
4. Crea una issue en GitHub con:
    - Descripción del problema
    - Logs relevantes
    - Pasos para reproducir

---

**¿Preguntas o sugerencias?** Abre un issue o PR. 🚀