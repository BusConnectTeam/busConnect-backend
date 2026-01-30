# BusConnect Backend - Arquitectura del Sistema

## 🏗️ System Architecture Overview

```mermaid
graph TB
    subgraph "External Clients"
        Web[Web App<br/>React/Angular]
        Mobile[Mobile App<br/>iOS/Android]
    end
    
    subgraph "API Layer - Port 8080"
        Gateway[API Gateway<br/>Spring Cloud Gateway<br/>Circuit Breaker + CORS]
    end
    
    subgraph "Service Discovery - Port 8761"
        Eureka[Eureka Server<br/>Netflix Eureka<br/>Service Registry]
    end
    
    subgraph "Business Services"
        UserService[User Service<br/>Port 8082<br/>Spring WebFlux + R2DBC<br/>Caffeine Cache]
        CatalogService[Catalog Service<br/>Port 8083<br/>Spring WebFlux + R2DBC<br/>OpenRouteService API]
        AuthService[Auth Service<br/>Port 8081<br/>🚧 En Desarrollo]
    end
    
    subgraph "Data Layer"
        PostgreSQL[(PostgreSQL 15+<br/>R2DBC Driver<br/><br/>Schemas:<br/>- user_service<br/>- catalog<br/>- auth)]
    end
    
    subgraph "External APIs"
        ORS[OpenRouteService<br/>Route Calculation<br/>2000 calls/day]
    end
    
    Web --> Gateway
    Mobile --> Gateway
    
    Gateway -.->|Register & Discover| Eureka
    UserService -.->|Register & Heartbeat| Eureka
    CatalogService -.->|Register & Heartbeat| Eureka
    AuthService -.->|Register & Heartbeat| Eureka
    
    Gateway -->|Route: /api/users/**| UserService
    Gateway -->|Route: /api/catalog/**| CatalogService
    Gateway -->|Route: /api/auth/**| AuthService
    
    UserService --> PostgreSQL
    CatalogService --> PostgreSQL
    AuthService --> PostgreSQL
    
    CatalogService -->|GET directions| ORS
    
    style Gateway fill:#1976D2,stroke:#0D47A1,stroke-width:3px,color:#fff
    style Eureka fill:#00897B,stroke:#004D40,stroke-width:3px,color:#fff
    style UserService fill:#5E35B1,stroke:#311B92,stroke-width:3px,color:#fff
    style CatalogService fill:#E64A19,stroke:#BF360C,stroke-width:3px,color:#fff
    style AuthService fill:#546E7A,stroke:#263238,stroke-width:3px,color:#fff
    style PostgreSQL fill:#336791,stroke:#1a3a52,stroke-width:3px,color:#fff
    style ORS fill:#00ACC1,stroke:#006064,stroke-width:3px,color:#fff
```

### Technology Stack

| Layer | Technology | Purpose |
|-------|------------|---------|
| **API Gateway** | Spring Cloud Gateway | Routing, Circuit Breaker, CORS |
| **Service Discovery** | Netflix Eureka | Service Registry & Load Balancing |
| **Microservices** | Spring Boot 3.3.13 + WebFlux | Reactive Non-blocking I/O |
| **Database Access** | Spring Data R2DBC | Reactive PostgreSQL Driver |
| **Database** | PostgreSQL 15+ | Persistent Storage |
| **Caching** | Caffeine | In-Memory Cache (L1) |
| **Resilience** | Resilience4j | Circuit Breaker + Timeout |
| **Containerization** | Docker + Docker Compose | Multi-container Orchestration |

---

## 🔄 End-to-End Request Flow

Flujo completo desde el cliente hasta la respuesta final:

```mermaid
sequenceDiagram
    participant Client as Client<br/>(Web/Mobile)
    participant Gateway as API Gateway<br/>:8080
    participant Eureka as Eureka Server<br/>:8761
    participant Catalog as Catalog Service<br/>:8083
    participant Cache as Caffeine Cache
    participant DB as PostgreSQL
    participant ORS as OpenRouteService

    Note over Client: User Request:<br/>Calculate Route
    Client->>Gateway: POST /api/catalog/calculate<br/>{originId: 08001, destinationId: 08015}
    
    Note over Gateway: 1. CORS Validation
    Gateway->>Gateway: Check Origin ✓
    
    Note over Gateway: 2. Route Matching
    Gateway->>Gateway: Match: /api/catalog/** → CATALOG-SERVICE
    
    Note over Gateway: 3. Service Discovery
    Gateway->>Eureka: GET /eureka/apps/CATALOG-SERVICE
    Eureka-->>Gateway: Instance: catalog-service:8083 (UP)
    
    Note over Gateway: 4. Circuit Breaker Check
    Gateway->>Gateway: Circuit State: CLOSED ✓
    
    Note over Gateway: 5. Forward Request
    Gateway->>Catalog: POST /calculate<br/>(rewrite path)
    
    Note over Catalog: 6. Validate Municipalities
    Catalog->>Cache: Get Municipality (08001)
    Cache-->>Catalog: Cache Hit ✓
    
    Catalog->>Cache: Get Municipality (08015)
    Cache-->>Catalog: Cache Miss
    Catalog->>DB: SELECT * FROM municipalities WHERE id=08015
    DB-->>Catalog: Municipality Data
    Catalog->>Cache: Store (24h TTL)
    
    Note over Catalog: 7. Check Route Cache
    Catalog->>Cache: Get Route (08001_08015_driving)
    Cache-->>Catalog: Cache Miss
    
    Note over Catalog: 8. External API Call
    Catalog->>ORS: POST /v2/directions/driving-car<br/>coordinates: [[2.1734,41.3851], [2.2416,41.3925]]
    ORS-->>Catalog: {distance: 8.5km, duration: 900s, geometry: ...}
    
    Note over Catalog: 9. Store Results
    Catalog->>Cache: Store Route (1h TTL)
    Catalog->>DB: INSERT INTO routes
    DB-->>Catalog: Saved
    
    Catalog-->>Gateway: 200 OK<br/>{route details}
    Gateway-->>Client: 200 OK<br/>{route details}
```

### Performance Metrics

| Scenario | Cache | Database | External API | Total Time | Status |
|----------|-------|----------|--------------|------------|--------|
| **Best Case** | ✅ Hit (route) | - | - | ~5-10ms | 🟢 Excellent |
| **Partial Hit** | ✅ Hit (municipalities) | - | ⚠️ ORS call | ~500-800ms | 🟡 Good |
| **Cache Miss** | ❌ Miss all | 2 queries | ⚠️ ORS call | ~1500-2000ms | 🟠 Acceptable |
| **Error Case** | - | - | ❌ ORS timeout | Circuit opens | 🔴 Degraded |

---

## 🐳 Deployment Architecture

Infraestructura Docker con redes y volúmenes:

```mermaid
graph TB
    subgraph "Docker Host"
        subgraph "busconnect-network (bridge)"
            subgraph "Service Discovery"
                EurekaContainer[eureka-service<br/>━━━━━━━━━━━━━<br/>Port: 8761<br/>Image: openjdk:21-slim<br/>━━━━━━━━━━━━━<br/>Health: /actuator/health]
            end
            
            subgraph "Gateway Layer"
                GatewayContainer[api-gateway<br/>━━━━━━━━━━━━━<br/>Port: 8080→8080<br/>Image: openjdk:21-slim<br/>━━━━━━━━━━━━━<br/>CORS + Circuit Breaker]
            end
            
            subgraph "Business Layer"
                UserContainer[user-service<br/>━━━━━━━━━━━━━<br/>Port: 8082<br/>Image: openjdk:21-slim<br/>━━━━━━━━━━━━━<br/>R2DBC + Caffeine]
                
                CatalogContainer[catalog-service<br/>━━━━━━━━━━━━━<br/>Port: 8083<br/>Image: openjdk:21-slim<br/>━━━━━━━━━━━━━<br/>R2DBC + OpenRouteService]
            end
            
            subgraph "Data Layer"
                PostgresContainer[postgres<br/>━━━━━━━━━━━━━<br/>Port: 5432→5432<br/>Image: postgres:15-alpine<br/>━━━━━━━━━━━━━<br/>Persistent Storage]
            end
        end
        
        subgraph "Volumes"
            PostgresVolume[(postgres-data<br/>Persistent Volume<br/>/var/lib/postgresql/data)]
        end
        
        subgraph "Environment"
            EnvFiles[.env Files<br/>- Root .env<br/>- catalog-service/.env<br/>- user-service/.env]
        end
    end
    
    subgraph "External"
        HostMachine[Host Machine<br/>localhost]
        Internet[Internet<br/>OpenRouteService API]
    end
    
    HostMachine -->|localhost:8080| GatewayContainer
    HostMachine -->|localhost:8761| EurekaContainer
    HostMachine -->|localhost:5432| PostgresContainer
    
    GatewayContainer --> EurekaContainer
    GatewayContainer --> UserContainer
    GatewayContainer --> CatalogContainer
    
    UserContainer --> EurekaContainer
    CatalogContainer --> EurekaContainer
    
    UserContainer --> PostgresContainer
    CatalogContainer --> PostgresContainer
    
    CatalogContainer -->|HTTPS| Internet
    
    PostgresContainer -.->|Mount| PostgresVolume
    
    GatewayContainer -.->|Read| EnvFiles
    UserContainer -.->|Read| EnvFiles
    CatalogContainer -.->|Read| EnvFiles
    PostgresContainer -.->|Read| EnvFiles
    
    style EurekaContainer fill:#00897B,stroke:#004D40,stroke-width:3px,color:#fff
    style GatewayContainer fill:#1976D2,stroke:#0D47A1,stroke-width:3px,color:#fff
    style UserContainer fill:#5E35B1,stroke:#311B92,stroke-width:3px,color:#fff
    style CatalogContainer fill:#E64A19,stroke:#BF360C,stroke-width:3px,color:#fff
    style PostgresContainer fill:#336791,stroke:#1a3a52,stroke-width:3px,color:#fff
    style PostgresVolume fill:#FFA000,stroke:#FF6F00,stroke-width:2px
    style HostMachine fill:#37474F,stroke:#263238,stroke-width:2px,color:#fff
    style Internet fill:#00ACC1,stroke:#006064,stroke-width:2px,color:#fff
```

### Container Startup Order

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Stage 1: Data Layer
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. postgres          
   ├─ Wait for healthy: pg_isready
   └─ Ready: Database accepting connections
   
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Stage 2: Service Discovery
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
2. eureka-service    
   ├─ Depends on: postgres
   ├─ Wait for: http://eureka-service:8761/actuator/health
   └─ Ready: Registry accepting registrations
   
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Stage 3: Business Services (Parallel)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
3a. user-service     
    ├─ Depends on: postgres + eureka-service
    └─ Auto-register with Eureka
    
3b. catalog-service  
    ├─ Depends on: postgres + eureka-service
    └─ Auto-register with Eureka
   
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Stage 4: API Gateway
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
4. api-gateway       
   ├─ Depends on: eureka-service
   ├─ Fetch service registry
   └─ Ready: Accepting client requests
```

### Network Configuration

```yaml
busconnect-network:
  driver: bridge
  ipam:
    config:
      - subnet: 172.20.0.0/16

Service IPs (auto-assigned):
- eureka-service:    172.20.0.2
- postgres:          172.20.0.3
- user-service:      172.20.0.4
- catalog-service:   172.20.0.5
- api-gateway:       172.20.0.6
```

### Port Mapping

| Service | Internal Port | External Port | Exposed to Host | Status |
|---------|--------------|---------------|-----------------|--------|
| **api-gateway** | 8080 | 8080 | ✅ Yes | 🟢 Public |
| **user-service** | 8082 | - | ❌ No (via Gateway) | 🔒 Internal |
| **catalog-service** | 8083 | - | ❌ No (via Gateway) | 🔒 Internal |
| **eureka-service** | 8761 | 8761 | ✅ Yes (Dashboard) | 🟡 Monitoring |
| **postgres** | 5432 | 5432 | ✅ Yes (Dev only) | ⚠️ Dev Only |

---

## 📦 Project Structure

```
busConnect-backend/
├── api-gateway/              # Spring Cloud Gateway (8080)
│   ├── ARCHITECTURE.md       # Gateway architecture details
│   ├── README.md
│   └── src/
├── catalog-service/          # Route calculation service (8083)
│   ├── ARCHITECTURE.md       # Catalog architecture details
│   ├── README.md
│   └── src/
├── eureka-service/           # Service discovery (8761)
│   ├── ARCHITECTURE.md       # Eureka architecture details
│   ├── README.md
│   └── src/
├── user-service/             # User management (8082)
│   ├── ARCHITECTURE.md       # User service architecture details
│   ├── README.md
│   └── src/
├── docker-compose.yml        # Multi-container orchestration
├── .env                      # Root environment variables
├── ARCHITECTURE.md           # 👈 This file (System overview)
└── README.md                 # Main documentation
```

---

## 🔍 Service Communication Patterns

### Synchronous Communication (HTTP/REST)

```
Client → API Gateway → Microservice
- Protocol: HTTP/1.1 over TCP
- Format: JSON
- Style: Request-Response
- Timeout: 10 seconds
- Retry: Circuit Breaker handles
```

### Service Discovery Pattern

```
Service Registration:
┌──────────────┐     register    ┌──────────────┐
│ Microservice │ ──────────────→ │ Eureka Server│
└──────────────┘                  └──────────────┘
       │                                 ▲
       │ heartbeat (every 30s)          │
       └─────────────────────────────────┘

Service Discovery:
┌─────────────┐   query      ┌──────────────┐
│ API Gateway │ ────────────→│ Eureka Server│
└─────────────┘              └──────────────┘
       │                            │
       │      service instances      │
       │◄────────────────────────────┘
       │
       ├──→ user-service:8082
       ├──→ catalog-service:8083
       └──→ ...
```

### Database Per Service Pattern

```
┌─────────────────┐     ┌──────────────────┐
│  user-service   │     │ catalog-service  │
└────────┬────────┘     └────────┬─────────┘
         │                       │
         │  user_service schema  │  catalog schema
         │                       │
         └───────────┬───────────┘
                     │
              ┌──────▼──────┐
              │  PostgreSQL │
              │   Database  │
              └─────────────┘

Isolation:
- Each service owns its schema
- No direct database access between services
- Service-to-service communication via REST APIs
```

---

## 🚀 Quick Start

```bash
# 1. Clone repository
git clone https://github.com/BusConnectTeam/busConnect-backend.git
cd busConnect-backend

# 2. Configure environment
cp .env.example .env
# Edit .env with your credentials

# 3. Start all services
docker-compose up -d

# 4. Verify services
curl http://localhost:8761           # Eureka Dashboard
curl http://localhost:8080/actuator/health  # Gateway Health
```

### Health Check URLs

```bash
# Eureka Server
http://localhost:8761

# API Gateway
http://localhost:8080/actuator/health

# User Service (via Gateway)
http://localhost:8080/api/users/actuator/health

# Catalog Service (via Gateway)
http://localhost:8080/api/catalog/actuator/health
```

---

## 🔗 Service Documentation

| Service | README | Architecture | Port |
|---------|--------|--------------|------|
| **API Gateway** | [README](./api-gateway/README.md) | [ARCHITECTURE](./api-gateway/ARCHITECTURE.md) | 8080 |
| **User Service** | [README](./user-service/README.md) | [ARCHITECTURE](./user-service/ARCHITECTURE.md) | 8082 |
| **Catalog Service** | [README](./catalog-service/README.md) | [ARCHITECTURE](./catalog-service/ARCHITECTURE.md) | 8083 |
| **Eureka Service** | [README](./eureka-service/README.md) | [ARCHITECTURE](./eureka-service/ARCHITECTURE.md) | 8761 |

---

## 📊 System Characteristics

### Scalability

```
Horizontal Scaling (Add instances):
┌──────────────┐
│ API Gateway  │ ───┐
└──────────────┘    │
                    ├──→ user-service-1:8082
┌──────────────┐    ├──→ user-service-2:8082
│   Eureka     │ ───┤
└──────────────┘    ├──→ catalog-service-1:8083
                    └──→ catalog-service-2:8083

Load Balancing: Round Robin (Ribbon)
```

### Resilience

| Pattern | Implementation | Purpose |
|---------|---------------|---------|
| **Circuit Breaker** | Resilience4j | Prevent cascading failures |
| **Timeout** | 10 seconds | Fail fast |
| **Fallback** | Graceful degradation | Return default response |
| **Retry** | Circuit breaker handles | Automatic retry logic |
| **Health Checks** | Spring Actuator | Monitor service status |

### Performance

| Metric | Target | Current |
|--------|--------|---------|
| Gateway Response Time (p95) | < 100ms | ~50ms (cached) |
| Service Response Time (p95) | < 500ms | ~200ms (cached) |
| Database Query Time (p95) | < 50ms | ~10-30ms |
| External API Call | < 2s | ~500-1500ms |
| Cache Hit Rate | > 80% | ~85-90% |

---

## 🔐 Security Considerations

```mermaid
graph LR
    Client[Client] --> HTTPS[HTTPS/TLS<br/>━━━━━━━━━━<br/>Transport Security]
    HTTPS --> Gateway[API Gateway<br/>━━━━━━━━━━<br/>CORS + Rate Limiting]
    Gateway --> Auth[Auth Service<br/>━━━━━━━━━━<br/>JWT Validation]
    Auth --> Services[Microservices<br/>━━━━━━━━━━<br/>Role-Based Access]
    Services --> DB[PostgreSQL<br/>━━━━━━━━━━<br/>Encrypted Connections]
    
    style Client fill:#37474F,stroke:#263238,stroke-width:2px,color:#fff
    style HTTPS fill:#43A047,stroke:#2E7D32,stroke-width:3px,color:#fff
    style Gateway fill:#FB8C00,stroke:#E65100,stroke-width:3px,color:#fff
    style Auth fill:#D32F2F,stroke:#B71C1C,stroke-width:3px,color:#fff
    style Services fill:#1976D2,stroke:#0D47A1,stroke-width:3px,color:#fff
    style DB fill:#5E35B1,stroke:#311B92,stroke-width:3px,color:#fff
```

**Security Status:**

| Feature | Status | Description |
|---------|--------|-------------|
| ✅ **CORS** | Implemented | Configured in API Gateway |
| ✅ **Environment Variables** | Implemented | Secrets in .env files |
| ✅ **DB Credentials** | Secured | Not in source code |
| ✅ **Network Isolation** | Implemented | Docker networks |
| 🚧 **JWT Authentication** | In Development | Auth service pending |
| 🚧 **HTTPS/TLS** | Pending | Production requirement |
| 🚧 **Rate Limiting** | Pending | DDoS protection |

---

## 🔗 Referencias

- [Main README](./README.md) - Setup y configuración general
- [Docker Setup](./DOCKER.md) - Instrucciones Docker detalladas
- [API Gateway Architecture](./api-gateway/ARCHITECTURE.md) - Detalles del gateway
- [Catalog Service Architecture](./catalog-service/ARCHITECTURE.md) - Sistema de rutas
- [User Service Architecture](./user-service/ARCHITECTURE.md) - Gestión de usuarios
- [Eureka Service Architecture](./eureka-service/ARCHITECTURE.md) - Service discovery
