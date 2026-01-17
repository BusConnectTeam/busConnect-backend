# 🚌 BusConnect Backend

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.13-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.3-blue.svg)](https://spring.io/projects/spring-cloud)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![Deploy to Render](https://github.com/BusConnectTeam/busConnect-backend/actions/workflows/deploy-render.yml/badge.svg?branch=develop)](https://github.com/BusConnectTeam/busConnect-backend/actions/workflows/deploy-render.yml)
[![License](https://img.shields.io/badge/License-Private-red.svg)]()

Plataforma modular para la gestión y búsqueda de rutas de autobuses en Catalunya, diseñada para ofrecer una experiencia simple, segura y moderna tanto para usuarios finales como para compañías de transporte. El sistema sigue una arquitectura de microservicios, enfocándose en escalabilidad, seguridad y monitoreo centralizado.

⚠️ **Nota**: BusConnect es un proyecto privado, no es una iniciativa de código abierto. Todos los derechos reservados al equipo de desarrollo.

---

## 📘 Tabla de Contenidos

- [Visión General](#-visión-general)
- [Arquitectura](#️-arquitectura) → 📐 [**Ver Arquitectura Completa**](./ARCHITECTURE.md)
- [Microservicios](#️-microservicios)
- [Stack Tecnológico](#-stack-tecnológico)
- [Inicio Rápido](#-inicio-rápido)
- [Configuración](#-configuración)
- [Documentación](#-documentación)
- [Seguridad](#-seguridad)
- [Estándares de Desarrollo](#-estándares-de-desarrollo)
- [Roadmap](#-roadmap)

---

## 🧩 Visión General

BusConnect proporciona una manera inteligente de buscar y gestionar rutas de autobuses por fecha, destino, número de pasajeros y tipo de autobús. Está construido sobre un ecosistema de microservicios que permite flexibilidad y escalabilidad modular para futuras expansiones.

### Características Principales

- 🔍 **Búsqueda Inteligente**: Rutas entre municipios de Catalunya con cálculo de distancia y duración
- 👥 **Gestión de Usuarios**: Sistema completo de usuarios con soft delete y roles
- 🗺️ **Catálogo Dinámico**: 947 municipios precargados con coordenadas GPS
- 🔄 **Arquitectura Reactiva**: Stack completamente no bloqueante con WebFlux y R2DBC
- 📊 **Service Discovery**: Eureka Server para registro y descubrimiento de servicios
- 🚪 **API Gateway**: Punto de entrada único con circuit breakers y CORS
- 🐳 **Dockerizado**: Todos los servicios containerizados y listos para producción
- 📝 **Documentación Automática**: Swagger UI en cada microservicio

---

## 🏗️ Arquitectura

### Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                         Clientes                                 │
│                  (Web, Mobile, Third-party)                      │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
                    ┌────────────────┐
                    │  API Gateway   │
                    │   (Port 8080)  │
                    │  - Routing     │
                    │  - CORS        │
                    │  - Circuit Br. │
                    └────────┬───────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
      ┌──────────┐   ┌──────────┐   ┌──────────┐
      │  User    │   │ Catalog  │   │  Future  │
      │ Service  │   │ Service  │   │ Services │
      │ (8082)   │   │ (8083)   │   │          │
      └────┬─────┘   └────┬─────┘   └──────────┘
           │              │
           └──────┬───────┘
                  │
                  ▼
         ┌────────────────┐
         │ Eureka Server  │
         │  (Port 8761)   │
         │ - Discovery    │
         │ - Registry     │
         └────────┬───────┘
                  │
      ┌───────────┴───────────┐
      │                       │
      ▼                       ▼
┌──────────┐          ┌──────────────┐
│PostgreSQL│          │OpenRouteServ.│
│  (5432)  │          │  (External)  │
└──────────┘          └──────────────┘
```

### Principios Arquitectónicos

- ✅ **Arquitectura de Microservicios**: Servicios independientes y escalables
- ✅ **Programación Reactiva**: Stack no bloqueante con Spring WebFlux y R2DBC
- ✅ **Service Discovery**: Registro dinámico con Netflix Eureka
- ✅ **API Gateway Pattern**: Punto de entrada único con Spring Cloud Gateway
- ✅ **Circuit Breaker**: Tolerancia a fallos con Resilience4j
- ✅ **Containerización**: Docker y Docker Compose para todos los servicios
- ✅ **Database per Service**: Esquemas independientes en PostgreSQL
- ✅ **Caché Distribuido**: Caffeine para optimización de rendimiento

---

## ⚙️ Microservicios

### 🚪 API Gateway
**Puerto**: `8080` | **Tecnología**: Spring Cloud Gateway (Reactivo)

Punto de entrada único para todos los microservicios. Proporciona enrutamiento inteligente, balanceo de carga, circuit breakers y gestión centralizada de CORS.

**Características**:
- Enrutamiento dinámico basado en Service Discovery
- Circuit breakers con Resilience4j (fallback automático)
- CORS configurado para múltiples orígenes
- Timeouts configurables (10s por defecto)
- Health checks expuestos vía Actuator

**Rutas**:
- `/api/users/**` → User Service
- `/api/catalog/**` → Catalog Service

📖 [Ver documentación completa](./api-gateway/README.md)

---

### 🔍 Catalog Service
**Puerto**: `8083` | **Tecnología**: Spring WebFlux + R2DBC

Microservicio reactivo para la gestión del catálogo de rutas de autobuses. Calcula rutas entre municipios de Catalunya usando OpenRouteService API.

**Características**:
- 947 municipios de Catalunya precargados con coordenadas GPS
- Integración con OpenRouteService para cálculo de rutas
- Caché de 2 niveles (rutas 1h, municipios 24h)
- Búsqueda por nombre y provincia
- Rate limiting (2000 peticiones/día)
- Modelo de datos completo: Route, Company, Schedule, VehicleType

**Endpoints principales**:
- `POST /api/routes/calculate` - Calcular ruta
- `GET /api/routes/municipalities` - Listar municipios
- `GET /api/routes/municipalities/search?name={name}` - Buscar municipio
- `GET /api/routes/health` - Health check

📖 [Ver documentación completa](./catalog-service/README.md)

---

### 👤 User Service
**Puerto**: `8082` | **Tecnología**: Spring WebFlux + R2DBC

Microservicio reactivo para la gestión completa de usuarios del sistema. Operaciones CRUD, soft delete y gestión de roles.

**Características**:
- CRUD completo de usuarios (reactivo)
- Soft delete (usuarios no se eliminan físicamente)
- Búsqueda avanzada (nombre, rol, email)
- Validación de email único
- Roles: ADMIN, USER, DRIVER, COMPANY
- Caché Caffeine (500 usuarios, 1h)

**Endpoints principales**:
- `POST /api/users` - Crear usuario
- `GET /api/users/{id}` - Obtener usuario
- `PUT /api/users/{id}` - Actualizar usuario
- `DELETE /api/users/{id}` - Soft delete
- `PUT /api/users/{id}/restore` - Restaurar usuario
- `GET /api/users/search/name?firstName={name}` - Buscar por nombre
- `GET /api/users/search/role?role={role}` - Filtrar por rol

📖 [Ver documentación completa](./user-service/README.md)

---

### 🔍 Eureka Service
**Puerto**: `8761` | **Tecnología**: Spring Cloud Netflix Eureka

Servidor de descubrimiento de servicios (Service Discovery). Actúa como registro centralizado para todos los microservicios.

**Características**:
- Registro automático de microservicios
- Health checks periódicos
- Balanceo de carga del lado del cliente
- Dashboard web para monitoreo
- Self-preservation mode configurable
- API REST para consultas programáticas

**Acceso**:
- Dashboard: `http://localhost:8761`
- API: `http://localhost:8761/eureka/apps`

📖 [Ver documentación completa](./eureka-service/README.md)

---

## 🛠 Stack Tecnológico

### Backend Core
- **Java**: 21 LTS (Eclipse Temurin)
- **Spring Boot**: 3.3.13
- **Spring Cloud**: 2023.0.3
- **Maven**: 3.9+

### Frameworks y Librerías
- **Spring WebFlux**: Programación reactiva
- **Spring Data R2DBC**: Acceso reactivo a PostgreSQL
- **Spring Cloud Gateway**: API Gateway reactivo
- **Spring Cloud Netflix Eureka**: Service Discovery
- **Resilience4j**: Circuit Breaker
- **Caffeine**: Caché en memoria de alto rendimiento
- **SpringDoc OpenAPI**: Documentación automática (Swagger)
- **Lombok**: Reducción de boilerplate
- **Jakarta Validation**: Validación de datos

### Base de Datos
- **PostgreSQL**: 15+ (driver R2DBC)
- **Flyway**: Migraciones de base de datos

### DevOps
- **Docker**: Containerización
- **Docker Compose**: Orquestación local
- **Git**: Control de versiones

### APIs Externas
- **OpenRouteService**: Cálculo de rutas geográficas

---

## 🚀 Inicio Rápido

### Prerequisitos

- **Java 21** (Eclipse Temurin recomendado)
- **Maven 3.9+**
- **Docker & Docker Compose**
- **PostgreSQL 15+** (o usar Docker Compose)

### 1. Clonar el Repositorio

```bash
git clone https://github.com/BusConnectTeam/busConnect-backend.git
cd busConnect-backend
```

### 2. Configurar Variables de Entorno

Copiar el archivo de ejemplo y configurar:

```bash
cp .env.example .env
```

Editar `.env` con tus credenciales:

```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=busconnectdb
DB_USERNAME=busconnect_user
DB_PASSWORD=your_secure_password

# OpenRouteService API
OPENROUTE_API_KEY=your_api_key_here

# Eureka
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://localhost:8761/eureka/
```

⚠️ **Importante**: Obtén tu API Key de OpenRouteService en https://openrouteservice.org/dev/#/signup

### 3. Iniciar con Docker Compose

#### Opción A: Todos los servicios

```bash
docker-compose up -d
```

#### Opción B: Servicios específicos

```bash
# Solo infraestructura
docker-compose up -d postgres eureka-service

# Agregar microservicios
docker-compose up -d user-service catalog-service

# Agregar gateway
docker-compose up -d api-gateway
```

### 4. Verificar que Todo Funcione

```bash
# Eureka Dashboard
curl http://localhost:8761

# API Gateway Health
curl http://localhost:8080/actuator/health

# User Service
curl http://localhost:8082/actuator/health

# Catalog Service
curl http://localhost:8083/actuator/health
```

### 5. Acceder a Swagger UI

- **User Service**: http://localhost:8082/swagger-ui.html
- **Catalog Service**: http://localhost:8083/swagger-ui.html
- **Eureka Dashboard**: http://localhost:8761

---

## ⚙️ Configuración

### Estructura de Configuración

```
busConnect-backend/
├── .env                          # Variables de entorno (NO COMMITEAR)
├── .env.example                  # Template de variables
├── docker-compose.yml            # Orquestación de servicios
├── pom.xml                       # Parent POM
├── api-gateway/
│   ├── src/main/resources/
│   │   └── application.yml       # Config del gateway
│   └── Dockerfile
├── catalog-service/
│   ├── src/main/resources/
│   │   └── application.yml       # Config del catalog
│   ├── .env                      # Variables locales (NO COMMITEAR)
│   └── Dockerfile
├── user-service/
│   ├── src/main/resources/
│   │   └── application.yml       # Config del user service
│   ├── .env.local                # Variables locales (NO COMMITEAR)
│   └── Dockerfile
└── eureka-service/
    ├── src/main/resources/
    │   └── application.yml       # Config de Eureka
    └── Dockerfile
```

### Archivos Excluidos de Git

El `.gitignore` está configurado para excluir:
- `.env`, `.env.local`, `*.env`
- Directorios `target/`
- Archivos de IDE (`.idea/`, `*.iml`)
- Datos de Docker (`postgres-data/`, `pgadmin-data/`)

### Ports Mapping

| Servicio | Puerto Local | Puerto Docker | Descripción |
|----------|--------------|---------------|-------------|
| API Gateway | 8080 | 8080 | Punto de entrada único |
| User Service | 8082 | 8082 | Gestión de usuarios |
| Catalog Service | 8083 | 8083 | Gestión de catálogo |
| Eureka Server | 8761 | 8761 | Service Discovery |
| PostgreSQL | 5432 | 5432 | Base de datos |

---

## 📚 Documentación

### Documentación de Microservicios

Cada microservicio tiene su propio README detallado:

- 🚪 [API Gateway](./api-gateway/README.md)
- 🔍 [Catalog Service](./catalog-service/README.md)
- 👤 [User Service](./user-service/README.md)
- 🔍 [Eureka Service](./eureka-service/README.md)

### Swagger UI

Documentación interactiva de APIs:

- **User Service**: http://localhost:8082/swagger-ui.html
- **Catalog Service**: http://localhost:8083/swagger-ui.html

### Eureka Dashboard

Monitoreo de servicios registrados:

- **Dashboard**: http://localhost:8761

---

## 🔐 Seguridad

### Prácticas Implementadas

- ✅ Variables de entorno para credenciales sensibles
- ✅ Archivos `.env` excluidos de Git y Docker
- ✅ Validación de entrada con Jakarta Validation
- ✅ Manejo centralizado de excepciones
- ✅ Logging sin información sensible (PII en DEBUG)
- ✅ Soft delete para auditoría de usuarios
- ✅ Circuit breakers para tolerancia a fallos
- ✅ Health checks en todos los servicios
- ✅ Usuarios no-root en contenedores Docker

### Roles del Sistema

| Rol | Descripción | Acceso |
|-----|-------------|--------|
| **ADMIN** | Administrador del sistema | Acceso completo |
| **USER** | Usuario estándar/cliente | Búsqueda y reservas |
| **DRIVER** | Conductor | Gestión de viajes |
| **COMPANY** | Representante de compañía | Gestión de rutas y vehículos |

### Próximas Implementaciones

- [ ] Servicio de autenticación (Auth Service)
- [ ] JWT para autenticación
- [ ] Rate limiting por usuario
- [ ] HTTPS/TLS en producción
- [ ] Encriptación de datos sensibles
- [ ] Auditoría de accesos

---

## 💻 Estándares de Desarrollo

### Convenciones de Código

- **Código**: Inglés (nombres de variables, métodos, clases)
- **Comentarios y JavaDoc**: Español
- **Commits**: Conventional Commits en inglés

### Estructura de Branches

```
main
├── develop
│   ├── feature/user-service
│   ├── feature/catalog-service
│   ├── feature/api-gateway
│   └── feature/auth-service
└── hotfix/[issue-number]
```

### Formato de Issues

```
[ServiceName #IssueNumber] Short description

Example: [UserService #23] Fix email validation
```

### Git Workflow

1. Crear branch desde `develop`: `feature/nombre-feature`
2. Desarrollar y hacer commits frecuentes
3. Crear Pull Request hacia `develop`
4. Code review por al menos 1 desarrollador
5. Merge después de aprobación

### Commits Convencionales

```bash
feat(user-service): add user creation endpoint
fix(catalog-service): resolve municipality search bug
refactor(api-gateway): improve routing configuration
docs(readme): update installation instructions
chore(deps): update Spring Boot to 3.3.13
```

### Testing

- Pruebas unitarias para lógica de negocio
- Pruebas de integración para endpoints
- Health checks en todos los servicios
- Testing manual con Postman/Insomnia

---

## 🗺️ Roadmap

### ✅ Completado

- [x] Arquitectura de microservicios
- [x] Eureka Server (Service Discovery)
- [x] API Gateway con circuit breakers
- [x] User Service (CRUD reactivo completo)
- [x] Catalog Service con 947 municipios
- [x] Integración con OpenRouteService
- [x] Dockerización de todos los servicios
- [x] Documentación Swagger
- [x] PostgreSQL con esquemas separados
- [x] Caché con Caffeine
- [x] Soft delete de usuarios
- [x] READMEs completos por servicio

### 🚧 En Progreso

- [ ] Auth Service (Autenticación y Autorización)
- [ ] Búsqueda avanzada de rutas
- [ ] Sistema de reservas
- [ ] Gestión de compañías de transporte

### 📋 Por Implementar

#### Backend
- [ ] Payment Service (Pagos)
- [ ] Notification Service (Emails, SMS)
- [ ] Booking Service (Reservas)
- [ ] Review Service (Valoraciones)
- [ ] Admin Dashboard Backend
- [ ] Integración con pasarelas de pago
- [ ] WebSockets para actualizaciones en tiempo real
- [ ] Sistema de mensajería (RabbitMQ/Kafka)

#### Infraestructura
- [ ] CI/CD con GitHub Actions
- [ ] Kubernetes para orquestación
- [ ] Monitoring con Prometheus + Grafana
- [ ] Centralized Logging (ELK Stack)
- [ ] API Documentation Portal
- [ ] Load testing y performance tuning

#### Frontend
- [ ] Landing Page (Búsqueda de rutas)
- [ ] Dashboard de Usuario
- [ ] Panel de Administración
- [ ] App Móvil (React Native)

#### Seguridad
- [ ] Autenticación JWT completa
- [ ] OAuth2 / OpenID Connect
- [ ] Rate limiting por usuario
- [ ] HTTPS en todos los servicios
- [ ] API Keys para clientes externos
- [ ] Auditoría completa de accesos

---

## 🎨 Experiencia de Usuario

### Landing Page (Planificado)

Los usuarios podrán:

- ✈️ Seleccionar fecha de viaje
- 🗺️ Elegir origen y destino
- 👥 Especificar número de pasajeros
- 🚌 Filtrar por tipo de autobús:
  - **Simple**: Básico
  - **Medium**: Confort medio (WiFi, AC)
  - **Lux**: Premium (WiFi, AC, asientos reclinables, toilet)

### Diseño Visual

- **Paleta de colores**: Por definir en iteraciones de diseño
- **Responsive**: Mobile-first approach
- **Accesibilidad**: WCAG 2.1 AA compliance
- **UI/UX**: Clean, modern, intuitive

---

## 🧪 Testing

### Ejecutar Tests

```bash
# Todos los tests
mvn test

# Tests de un servicio específico
cd user-service
mvn test

# Test con coverage
mvn test jacoco:report
```

### Testing Manual

#### 1. Crear Usuario

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User",
    "phone": "+34612345678",
    "role": "USER"
  }'
```

#### 2. Calcular Ruta

```bash
curl -X POST http://localhost:8080/api/catalog/calculate \
  -H "Content-Type: application/json" \
  -d '{
    "origin": "Barcelona",
    "destination": "Girona"
  }'
```

#### 3. Listar Municipios

```bash
curl http://localhost:8080/api/catalog/municipalities
```

---

## 🐛 Troubleshooting

### Problema: Servicios no se registran en Eureka

**Solución**:
```bash
# Verificar que Eureka esté corriendo
curl http://localhost:8761/actuator/health

# Ver logs del servicio
docker logs user-service

# Verificar configuración de Eureka en application.yml
```

### Problema: Error de conexión a PostgreSQL

**Solución**:
```bash
# Verificar que PostgreSQL esté corriendo
docker ps | grep postgres

# Verificar logs
docker logs postgres

# Conectar manualmente
psql -h localhost -U busconnect_user -d busconnectdb
```

### Problema: Puertos ya en uso

**Solución**:
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID [PID] /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

---

## 📞 Soporte y Contacto

Para preguntas, issues o sugerencias:

1. Crear un Issue en GitHub con el template apropiado
2. Contactar al equipo de desarrollo
3. Consultar la documentación de cada microservicio

---

## 📄 Licencia

⚠️ **Proyecto Privado** - Todos los derechos reservados al equipo de desarrollo de BusConnect.

Este proyecto NO es de código abierto. El código fuente, la documentación y todos los recursos relacionados son propiedad exclusiva del equipo BusConnectTeam.

---

## 👥 Equipo de Desarrollo

Desarrollado con ❤️ por el equipo BusConnectTeam

---

**Última actualización**: 17 de enero de 2026
