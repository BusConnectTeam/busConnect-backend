# 🚌 BusConnect Backend

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.13-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.3-blue.svg)](https://spring.io/projects/spring-cloud)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![Deploy](https://github.com/BusConnectTeam/busConnect-backend/actions/workflows/deploy-render.yml/badge.svg?branch=develop)](https://github.com/BusConnectTeam/busConnect-backend/actions/workflows/deploy-render.yml)
[![License](https://img.shields.io/badge/License-Private-red.svg)]()

Plataforma modular para gestión y búsqueda de rutas de autobuses en Catalunya. Arquitectura de microservicios reactivos con Spring Cloud, enfocada en escalabilidad y alta disponibilidad.

**📐 [Ver Arquitectura Completa del Sistema](./ARCHITECTURE.md)**

---

## 📘 Contenido

- [Características](#-características)
- [Microservicios](#-microservicios)
- [Stack Tecnológico](#-stack-tecnológico)
- [Inicio Rápido](#-inicio-rápido)
- [Roadmap](#-roadmap)
- [Equipo](#-equipo)

---

## ✨ Características

- 🔍 **Búsqueda Inteligente** - Cálculo de rutas entre 947 municipios de Catalunya
- 👥 **Gestión de Usuarios** - CRUD completo con soft delete y roles (ADMIN, USER, DRIVER, COMPANY)
- 🔄 **Arquitectura Reactiva** - Spring WebFlux + R2DBC para operaciones no bloqueantes
- 🚪 **API Gateway** - Punto de entrada único con circuit breakers y CORS
- 📊 **Service Discovery** - Netflix Eureka para registro dinámico de servicios
- ⚡ **Caché Inteligente** - Caffeine con TTL configurables (rutas 1h, municipios 24h)
- 🐳 **Containerizado** - Docker Compose para desarrollo y producción
- 📝 **Documentación Automática** - Swagger UI en cada microservicio

---

## ⚙️ Microservicios

| Servicio | Puerto | Descripción | Documentación |
|----------|--------|-------------|---------------|
| **🚪 API Gateway** | 8080 | Enrutamiento, Circuit Breaker, CORS | [README](./api-gateway/README.md) \| [ARCH](./api-gateway/ARCHITECTURE.md) |
| **🔍 Catalog Service** | 8083 | 947 municipios, cálculo de rutas (OpenRouteService) | [README](./catalog-service/README.md) \| [ARCH](./catalog-service/ARCHITECTURE.md) |
| **👤 User Service** | 8082 | CRUD usuarios, soft delete, roles | [README](./user-service/README.md) \| [ARCH](./user-service/ARCHITECTURE.md) |
| **📡 Eureka Service** | 8761 | Service Discovery & Registry | [README](./eureka-service/README.md) \| [ARCH](./eureka-service/ARCHITECTURE.md) |

---

## 🛠 Stack Tecnológico

| Capa | Tecnología |
|------|------------|
| **Backend** | Java 21, Spring Boot 3.3.13, Spring Cloud 2023.0.3 |
| **Reactive** | Spring WebFlux, Spring Data R2DBC |
| **Gateway** | Spring Cloud Gateway, Resilience4j |
| **Discovery** | Netflix Eureka |
| **Database** | PostgreSQL 15+, Flyway |
| **Cache** | Caffeine |
| **Docs** | SpringDoc OpenAPI (Swagger) |
| **DevOps** | Docker, Docker Compose, GitHub Actions |
| **External APIs** | OpenRouteService |

---

## 🚀 Inicio Rápido

### Prerequisitos

```bash
Java 21, Maven 3.9+, Docker & Docker Compose
```

### 1. Clonar y Configurar

```bash
git clone https://github.com/BusConnectTeam/busConnect-backend.git
cd busConnect-backend
cp .env.example .env
```

Editar `.env` con tus credenciales:
```env
DB_PASSWORD=your_secure_password
OPENROUTE_API_KEY=your_api_key_here  # Obtener en https://openrouteservice.org
```

### 2. Iniciar Servicios

```bash
docker-compose up -d
```

### 3. Verificar

```bash
# Eureka Dashboard
http://localhost:8761

# Swagger UI
http://localhost:8082/swagger-ui.html  # User Service
http://localhost:8083/swagger-ui.html  # Catalog Service

# Health Checks
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8082/actuator/health  # User Service
curl http://localhost:8083/actuator/health  # Catalog Service
```

### Puertos

| Servicio | Puerto | Expuesto |
|----------|--------|----------|
| API Gateway | 8080 | ✅ Public |
| User Service | 8082 | 🔒 Internal |
| Catalog Service | 8083 | 🔒 Internal |
| Eureka | 8761 | 🟡 Dashboard |
| PostgreSQL | 5432 | ⚠️ Dev only |

---

## 🗺️ Roadmap

### ✅ Fase 1 - Core (Completado)
- API Gateway con circuit breakers
- User Service (CRUD completo)
- Catalog Service (947 municipios + OpenRouteService)
- Eureka Service Discovery
- PostgreSQL con esquemas separados
- Caché Caffeine multinivel
- Dockerización completa

### 🚧 Fase 2 - Autenticación (En Progreso)
- Auth Service con JWT
- OAuth2 / OpenID Connect
- Rate limiting por usuario

### 📋 Fase 3 - Negocio
- Booking Service (reservas)
- Payment Service (pagos)
- Notification Service (emails/SMS)
- Review Service (valoraciones)

### 🚀 Fase 4 - Producción
- CI/CD con GitHub Actions
- Kubernetes
- Monitoring (Prometheus + Grafana)
- Centralized Logging (ELK)

---

## 👥 Equipo

**Desarrollado con ❤️ por:**

| Nombre | Rol | GitHub |
|--------|-----|--------|
| **Ainoha Barcia** | Backend Developer | [@AinohaBarcia](https://github.com/AinohaBarcia) |
| **Gabriela Bustamante** | Backend Developer & DevOps / AI Integration | [@GabyB73](https://github.com/GabyB73) |
| **Irina Ichim** | Full Stack Developer & DevOps / AI Integration | [@IrinaIchim](https://github.com/IrinaIchim) |

### Agradecimientos Especiales

Gracias a **Ainoha**, **Gabriela** e **Irina** por su dedicación, trabajo en equipo y excelentes contribuciones al desarrollo de BusConnect. Este proyecto no sería posible sin su esfuerzo y profesionalismo. 🎉

---

## 📄 Licencia

⚠️ **Proyecto Privado** - Todos los derechos reservados a BusConnectTeam.

---

**Última actualización**: Enero 2026
