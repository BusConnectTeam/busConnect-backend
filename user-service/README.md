# User Service - BusConnect

## 📋 Descripción

Microservicio reactivo para la gestión completa de usuarios del sistema BusConnect. Proporciona operaciones CRUD, soft delete, búsquedas avanzadas y gestión de roles de usuario utilizando una arquitectura totalmente reactiva con Spring WebFlux y R2DBC.

## 🏗️ Arquitectura

Este servicio implementa una **arquitectura completamente reactiva**:

- **Spring WebFlux**: Framework reactivo no bloqueante
- **Spring Data R2DBC**: Acceso reactivo a PostgreSQL
- **Programación Reactiva**: Uso de Mono y Flux
- **Soft Delete**: Los usuarios no se eliminan físicamente
- **Caché Caffeine**: Optimización de consultas frecuentes
- **Validación Reactiva**: Validación de datos con Jakarta Validation

## 🔧 Tecnologías

- **Spring Boot**: 3.3.13
- **Spring WebFlux**: Programación reactiva
- **Spring Data R2DBC**: Base de datos reactiva
- **PostgreSQL**: Base de datos (driver R2DBC)
- **Caffeine**: Caché en memoria
- **SpringDoc OpenAPI**: Documentación automática (Swagger)
- **Lombok**: Reducción de código boilerplate
- **Jakarta Validation**: Validación de datos

## 📦 Configuración

### Variables de Entorno

| Variable | Descripción | Valor por Defecto | Requerida |
|----------|-------------|-------------------|-----------|
| `DB_HOST` | Host de PostgreSQL | `localhost` | ✅ |
| `DB_PORT` | Puerto de PostgreSQL | `5432` | ✅ |
| `DB_NAME` | Nombre de la base de datos | `busconnectdb` | ✅ |
| `DB_USERNAME` | Usuario de PostgreSQL | `busconnect_user` | ✅ |
| `DB_PASSWORD` | Contraseña de PostgreSQL | - | ✅ |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | URL de Eureka Server | `http://localhost:8761/eureka/` | ⚠️ |

### Configuración del Caché

```yaml
cache:
  type: caffeine
  caffeine:
    spec: maximumSize=500,expireAfterWrite=1h
  cache-names:
    - users
    - sessions
```

### Puertos

- **Puerto del servicio**: `8082`
- **PostgreSQL**: `5432`
- **Eureka Server**: `8761`

## 🗄️ Modelo de Datos

### Entidad User

```java
@Table(name = "users", schema = "user_service")
public class User {
    private Long id;
    private String email;              // Único, requerido
    private String firstName;
    private String lastName;
    private String phone;
    private UserRole role;             // ADMIN, USER, DRIVER, etc.
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;          // Para soft delete
}
```

### Roles de Usuario

```java
public enum UserRole {
    ADMIN,      // Administrador del sistema
    USER,       // Usuario estándar
    DRIVER,     // Conductor
    COMPANY     // Representante de compañía
}
```

## 🚀 Endpoints API

### 👤 Gestión de Usuarios

#### POST `/api/users`
Crear un nuevo usuario.

**Request Body:**
```json
{
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+34612345678",
  "role": "USER"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+34612345678",
  "role": "USER",
  "createdAt": "2026-01-17T10:30:00",
  "updatedAt": "2026-01-17T10:30:00",
  "active": true
}
```

**Validaciones:**
- ✅ Email válido y único
- ✅ Todos los campos requeridos
- ✅ Role válido

**Códigos de Estado:**
- `201 Created`: Usuario creado exitosamente
- `400 Bad Request`: Datos de entrada inválidos
- `409 Conflict`: Email ya existe

#### GET `/api/users/{id}`
Obtener usuario por ID.

**Response (200 OK):**
```json
{
  "id": 1,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+34612345678",
  "role": "USER",
  "createdAt": "2026-01-17T10:30:00",
  "updatedAt": "2026-01-17T10:30:00",
  "active": true
}
```

**Códigos de Estado:**
- `200 OK`: Usuario encontrado
- `404 Not Found`: Usuario no existe

#### GET `/api/users/email/{email}`
Obtener usuario por email.

**Ejemplo:**
```
GET /api/users/email/user@example.com
```

#### GET `/api/users`
Listar todos los usuarios activos.

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "email": "user1@example.com",
    "firstName": "John",
    "lastName": "Doe",
    ...
  },
  {
    "id": 2,
    "email": "user2@example.com",
    "firstName": "Jane",
    "lastName": "Smith",
    ...
  }
]
```

#### PUT `/api/users/{id}`
Actualizar un usuario existente.

**Request Body:**
```json
{
  "email": "newemail@example.com",
  "firstName": "John",
  "lastName": "Doe Updated",
  "phone": "+34698765432",
  "role": "ADMIN"
}
```

**Validaciones:**
- ✅ Email único (si se cambia)
- ✅ Usuario debe existir
- ✅ Usuario debe estar activo

**Códigos de Estado:**
- `200 OK`: Usuario actualizado
- `400 Bad Request`: Datos inválidos
- `404 Not Found`: Usuario no existe
- `409 Conflict`: Email ya en uso

#### DELETE `/api/users/{id}`
Soft delete de un usuario (marca como inactivo).

**Response (204 No Content)**

⚠️ **Nota**: El usuario NO se elimina de la base de datos, solo se marca como `isActive = false`.

**Códigos de Estado:**
- `204 No Content`: Usuario desactivado
- `404 Not Found`: Usuario no existe

#### PUT `/api/users/{id}/restore`
Restaurar un usuario desactivado.

**Response (200 OK):**
```json
{
  "id": 1,
  "email": "user@example.com",
  "active": true,
  ...
}
```

**Validaciones:**
- ✅ Usuario debe existir
- ✅ Usuario debe estar inactivo

**Códigos de Estado:**
- `200 OK`: Usuario restaurado
- `400 Bad Request`: Usuario ya está activo
- `404 Not Found`: Usuario no existe

### 🔍 Búsquedas Avanzadas

#### GET `/api/users/search/name?firstName={name}`
Buscar usuarios por nombre (case-insensitive, parcial).

**Ejemplo:**
```
GET /api/users/search/name?firstName=John
```

#### GET `/api/users/search/role?role={role}`
Filtrar usuarios por rol.

**Ejemplo:**
```
GET /api/users/search/role?role=ADMIN
```

**Roles válidos**: `ADMIN`, `USER`, `DRIVER`, `COMPANY`

#### GET `/api/users/inactive`
Listar todos los usuarios desactivados (soft deleted).

**Response:**
```json
[
  {
    "id": 5,
    "email": "deleted@example.com",
    "active": false,
    ...
  }
]
```

### 📊 Monitoreo

#### GET `/actuator/health`
Health check del servicio.

```bash
curl http://localhost:8082/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "r2dbc": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "SELECT 1"
      }
    }
  }
}
```

#### GET `/actuator/metrics`
Métricas del servicio.

## 📚 Documentación API (Swagger)

Acceder a la documentación interactiva:

- **Swagger UI**: `http://localhost:8082/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8082/api-docs`

## 🐳 Docker

### Dockerfile

El servicio incluye un Dockerfile multi-stage optimizado:

```dockerfile
# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Construcción de la imagen

```bash
docker build -t busconnect/user-service:latest .
```

### Ejecución con Docker

```bash
docker run -d \
  -p 8082:8082 \
  -e DB_HOST=postgres \
  -e DB_PORT=5432 \
  -e DB_NAME=busconnectdb \
  -e DB_USERNAME=busconnect_user \
  -e DB_PASSWORD=your_password \
  --name user-service \
  busconnect/user-service:latest
```

## 🚀 Ejecución

### Desarrollo Local

**Requisitos previos:**
- Java 21
- Maven 3.9+
- PostgreSQL 15+

**Pasos:**

1. **Configurar variables de entorno** (crear `.env.local`):
```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=busconnectdb
DB_USERNAME=busconnect_user
DB_PASSWORD=your_password
```

2. **Ejecutar con Maven**:
```bash
mvn spring-boot:run
```

3. **O compilar y ejecutar JAR**:
```bash
mvn clean package
java -jar target/user-service-0.0.1-SNAPSHOT.jar
```

### Con Docker Compose

```bash
docker-compose up user-service
```

## 💾 Base de Datos

### Esquema

El servicio utiliza el esquema `user_service` en PostgreSQL.

### Tabla Users

```sql
CREATE TABLE user_service.users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_users_email ON user_service.users(email);
CREATE INDEX idx_users_active ON user_service.users(is_active);
CREATE INDEX idx_users_role ON user_service.users(role);
```

### Migraciones

Las migraciones se manejarán con Flyway en el futuro:
- `V1__create_users_table.sql`
- `V2__add_user_indexes.sql`

## ⚡ Optimización y Rendimiento

### Sistema de Caché

Caché en memoria con Caffeine:

- **Capacidad máxima**: 500 usuarios
- **Expiración**: 1 hora después de escritura
- **Estrategia**: Write-through cache

### Soft Delete

**Ventajas:**
- ✅ Recuperación de usuarios eliminados
- ✅ Auditoría completa
- ✅ Integridad referencial
- ✅ Análisis histórico

**Desventaja:**
- ⚠️ Crecimiento de tabla (mitigado con índices)

### Programación Reactiva

**Beneficios:**
- ✅ No bloqueante
- ✅ Escalable
- ✅ Eficiente en I/O
- ✅ Backpressure automático

## 🧪 Pruebas

### Crear Usuario

```bash
curl -X POST http://localhost:8082/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User",
    "phone": "+34612345678",
    "role": "USER"
  }'
```

### Obtener Usuario

```bash
# Por ID
curl http://localhost:8082/api/users/1

# Por email
curl http://localhost:8082/api/users/email/test@example.com
```

### Listar Usuarios

```bash
# Todos los activos
curl http://localhost:8082/api/users

# Por nombre
curl "http://localhost:8082/api/users/search/name?firstName=Test"

# Por rol
curl "http://localhost:8082/api/users/search/role?role=ADMIN"

# Inactivos
curl http://localhost:8082/api/users/inactive
```

### Actualizar Usuario

```bash
curl -X PUT http://localhost:8082/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{
    "email": "updated@example.com",
    "firstName": "Updated",
    "lastName": "User",
    "phone": "+34698765432",
    "role": "ADMIN"
  }'
```

### Soft Delete

```bash
curl -X DELETE http://localhost:8082/api/users/1
```

### Restaurar Usuario

```bash
curl -X PUT http://localhost:8082/api/users/1/restore
```

### Health Check

```bash
curl http://localhost:8082/actuator/health
```

## 📝 Estructura del Proyecto

```
user-service/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/busconnect/userservice/
│       │       ├── UserServiceApplication.java
│       │       ├── controller/
│       │       │   └── UserController.java
│       │       ├── dto/
│       │       │   ├── error/
│       │       │   │   └── ErrorResponse.java
│       │       │   ├── request/
│       │       │   │   ├── CreateUserRequest.java
│       │       │   │   └── UpdateUserRequest.java
│       │       │   └── response/
│       │       │       └── UserResponse.java
│       │       ├── exception/
│       │       │   ├── GlobalExceptionHandler.java
│       │       │   ├── UserNotFoundException.java
│       │       │   ├── EmailAlreadyExistsException.java
│       │       │   └── UserAlreadyActiveException.java
│       │       ├── model/
│       │       │   ├── User.java
│       │       │   └── UserRole.java
│       │       ├── repository/
│       │       │   └── UserRepository.java
│       │       └── service/
│       │           ├── UserService.java
│       │           └── UserServiceImpl.java
│       └── resources/
│           ├── application.yml
│           └── messages.properties
├── .dockerignore
├── .env.local
├── Dockerfile
├── pom.xml
└── README.md
```

## 🔐 Seguridad

### Buenas Prácticas Implementadas

- ✅ Variables de entorno para credenciales
- ✅ `.env` excluido de Git y Docker
- ✅ Validación de entrada con Jakarta Validation
- ✅ Manejo centralizado de excepciones
- ✅ Logging sin información sensible (PII en DEBUG)
- ✅ Soft delete para auditoría
- ✅ Email único validado

### Próximas Mejoras de Seguridad

- [ ] Autenticación JWT
- [ ] Encriptación de datos sensibles
- [ ] Rate limiting por usuario
- [ ] Auditoría de accesos
- [ ] HTTPS en producción
- [ ] Gestión de contraseñas (actualmente delegado al servicio de autenticación)

## 🐛 Troubleshooting

### Error: "Email already exists"

**Causa**: El email ya está registrado en el sistema.

**Solución**:
- Verificar si el usuario existe: `GET /api/users/email/{email}`
- Usar un email diferente
- Si el usuario está inactivo, restaurarlo: `PUT /api/users/{id}/restore`

### Error: "User not found"

**Causa**: El usuario con ese ID no existe o está inactivo.

**Solución**:
```bash
# Verificar si existe en usuarios inactivos
curl http://localhost:8082/api/users/inactive

# Restaurar si existe
curl -X PUT http://localhost:8082/api/users/{id}/restore
```

### Error: Conexión a PostgreSQL

**Causa**: PostgreSQL no está disponible o credenciales incorrectas.

**Solución**:
```bash
# Verificar PostgreSQL
docker ps | grep postgres

# Verificar conexión
psql -h localhost -U busconnect_user -d busconnectdb

# Verificar esquema
\dn user_service
```

### Error: "User already active"

**Causa**: Intentando restaurar un usuario que ya está activo.

**Solución**: El usuario ya está disponible, no necesita restauración.

## 📊 Monitoreo

### Logs

```bash
# Ver logs en Docker
docker logs -f user-service

# Filtrar por nivel
docker logs user-service 2>&1 | grep ERROR
```

### Nivel de Logs Configurado

```yaml
logging:
  level:
    com.busconnect.userservice: DEBUG
    io.r2dbc.postgresql.QUERY: DEBUG
    io.r2dbc.postgresql.PARAM: DEBUG
    org.springframework.r2dbc: DEBUG
```

### Estrategia de Logging

- **DEBUG**: Operaciones rutinarias, emails (PII)
- **ERROR**: Manejo de excepciones
- **Logs de éxito**: En la capa de servicio (evita duplicación)

## 🔄 Integración con Otros Servicios

### Service Discovery (Eureka)

El servicio se registra automáticamente en Eureka:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://eureka-service:8761/eureka/
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${random.value}
```

### API Gateway

Las peticiones pasan por el API Gateway:

```
Cliente → API Gateway (8080) → User Service (8082)
```

**Ruta**: `/api/users/**` → `user-service`

## 📄 Licencia

Este proyecto es parte del sistema BusConnect.

## 🤝 Contribución

Para contribuir al servicio, consultar el [README principal](../README.md) y seguir las guías de estilo del proyecto.

## 📚 Recursos Adicionales

- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
- [Spring Data R2DBC](https://spring.io/projects/spring-data-r2dbc)
- [Project Reactor](https://projectreactor.io/)
- [Reactive Programming Guide](https://www.reactivemanifesto.org/)
