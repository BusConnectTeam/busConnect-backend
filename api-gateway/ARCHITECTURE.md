# API Gateway - Arquitectura

## 🔄 Flujo de Request

Cómo viaja una petición desde el cliente hasta los microservicios:

```mermaid
sequenceDiagram
    participant Cliente
    participant Gateway as API Gateway<br/>(8080)
    participant Eureka as Eureka Server<br/>(8761)
    participant User as User Service<br/>(8082)
    participant Catalog as Catalog Service<br/>(8083)
    
    Cliente->>Gateway: POST /api/users<br/>{user data}
    
    Note over Gateway: 1. CORS Check
    Note over Gateway: 2. Route Match<br/>/api/users/** → USER-SERVICE
    
    Gateway->>Eureka: ¿Dónde está USER-SERVICE?
    Eureka-->>Gateway: Instance: user-service:8082
    
    Note over Gateway: 3. Circuit Breaker Check<br/>(CLOSED ✓)
    
    Gateway->>User: Forward Request<br/>Rewrite: /api/users → /users
    User-->>User: Process Request
    User-->>Gateway: 201 Created<br/>{user response}
    
    Note over Gateway: 4. Dedupe Headers
    
    Gateway-->>Cliente: 201 Created<br/>{user response}
```

---

## ⚡ Circuit Breaker States

Estados del Circuit Breaker con Resilience4j:

```mermaid
stateDiagram-v2
    [*] --> CLOSED: Inicio
    
    CLOSED --> OPEN: 50% de fallos<br/>en 10 llamadas
    OPEN --> HALF_OPEN: Después de 10s
    HALF_OPEN --> CLOSED: 5 llamadas exitosas
    HALF_OPEN --> OPEN: Cualquier fallo
    
    CLOSED: 🟢 CLOSED<br/>Tráfico normal<br/>Monitorea fallos
    
    OPEN: 🔴 OPEN<br/>Circuit abierto<br/>→ Fallback inmediato<br/>No llama al servicio
    
    HALF_OPEN: 🟡 HALF_OPEN<br/>Probando recuperación<br/>Permite 5 llamadas de prueba
    
    note right of OPEN
        Fallback Response:
        {
          "error": "Service unavailable",
          "fallback": true
        }
    end note
```

### Configuración

| Parámetro | Valor | Descripción |
|-----------|-------|-------------|
| Sliding Window | 10 llamadas | Ventana de medición |
| Failure Rate Threshold | 50% | Umbral de fallos |
| Wait Duration | 10s | Tiempo en OPEN |
| Half-Open Calls | 5 | Llamadas de prueba |
| Timeout | 10s | Timeout por llamada |

---

## 🔍 Service Discovery Flow

Integración con Eureka Server:

```mermaid
flowchart TB
    Start([Cliente hace Request]) --> Gateway[API Gateway]
    
    Gateway --> Parse{Parse URL}
    
    Parse -->|/api/users/**| UserRoute[Route: USER-SERVICE]
    Parse -->|/api/catalog/**| CatalogRoute[Route: CATALOG-SERVICE]
    
    UserRoute --> Eureka1[Consultar Eureka<br/>USER-SERVICE]
    CatalogRoute --> Eureka2[Consultar Eureka<br/>CATALOG-SERVICE]
    
    Eureka1 --> Cache1{¿En caché?}
    Eureka2 --> Cache2{¿En caché?}
    
    Cache1 -->|Sí| Instance1[user-service:8082]
    Cache1 -->|No| Fetch1[Fetch from Eureka]
    Fetch1 --> Instance1
    
    Cache2 -->|Sí| Instance2[catalog-service:8083]
    Cache2 -->|No| Fetch2[Fetch from Eureka]
    Fetch2 --> Instance2
    
    Instance1 --> LB1[Load Balance<br/>si múltiples instancias]
    Instance2 --> LB2[Load Balance<br/>si múltiples instancias]
    
    LB1 --> Forward1[Forward Request]
    LB2 --> Forward2[Forward Request]
    
    Forward1 --> Response1[Response]
    Forward2 --> Response2[Response]
    
    Response1 --> End([Return to Cliente])
    Response2 --> End
    
    style Gateway fill:#4CAF50,stroke:#2E7D32,color:#fff
    style Eureka1 fill:#2196F3,stroke:#1565C0,color:#fff
    style Eureka2 fill:#2196F3,stroke:#1565C0,color:#fff
    style Instance1 fill:#FF9800,stroke:#E65100,color:#fff
    style Instance2 fill:#FF9800,stroke:#E65100,color:#fff
```

### Load Balancing

Cuando hay múltiples instancias del mismo servicio:

```
USER-SERVICE:
├── instance-1: user-service:8082 (10.0.0.1)
├── instance-2: user-service:8082 (10.0.0.2)
└── instance-3: user-service:8082 (10.0.0.3)

Gateway selecciona usando Round Robin
```

---

## 🚦 Route Configuration

### Path Rewriting

```
Cliente → Gateway       Gateway → Service
─────────────────       ─────────────────
/api/users/123    →     /123
/api/catalog/routes →   /routes
```

### Filters Chain

```mermaid
graph LR
    Request[Request] --> F1[DedupeResponseHeader]
    F1 --> F2[RewritePath]
    F2 --> F3[CircuitBreaker]
    F3 --> Service[Target Service]
    
    Service --> R3[CircuitBreaker]
    R3 --> R2[RewritePath]
    R2 --> R1[DedupeResponseHeader]
    R1 --> Response[Response]
    
    style F3 fill:#f44336,color:#fff
    style R3 fill:#f44336,color:#fff
```

---

## 🔐 CORS Configuration

```mermaid
flowchart LR
    Browser[Browser<br/>localhost:3000] -->|Preflight<br/>OPTIONS| Gateway[API Gateway]
    
    Gateway --> CORS{CORS Check}
    
    CORS -->|Allow Origin?| CheckOrigin[✓ localhost:3000<br/>✓ localhost:4200<br/>✓ localhost:5173]
    CheckOrigin -->|Allowed| Headers[Add Headers:<br/>Access-Control-Allow-Origin<br/>Access-Control-Allow-Methods<br/>Access-Control-Allow-Credentials]
    
    Headers -->|200 OK| Browser
    
    CORS -->|Deny| Reject[403 Forbidden]
    Reject --> Browser
    
    style CORS fill:#9C27B0,color:#fff
    style CheckOrigin fill:#4CAF50,color:#fff
    style Reject fill:#f44336,color:#fff
```

**Allowed Origins:**
- `http://localhost:3000` (React)
- `http://localhost:4200` (Angular)
- `http://localhost:5173` (Vite)

**Allowed Methods:**
- GET, POST, PUT, DELETE, PATCH, OPTIONS

**Credentials:** Enabled

---

## 📊 Health Check Flow

```mermaid
flowchart TB
    Monitor[Monitoring System] -->|GET /actuator/health| Gateway
    
    Gateway --> CheckEureka[Check Eureka Connection]
    Gateway --> CheckRoutes[Check Routes Config]
    Gateway --> CheckCircuit[Check Circuit Breakers]
    
    CheckEureka -->|Connected| E1[✓ Eureka: UP]
    CheckEureka -->|Failed| E2[✗ Eureka: DOWN]
    
    CheckRoutes -->|Valid| R1[✓ Routes: OK]
    CheckRoutes -->|Invalid| R2[✗ Routes: ERROR]
    
    CheckCircuit -->|All CLOSED| C1[✓ Circuits: HEALTHY]
    CheckCircuit -->|Some OPEN| C2[⚠ Circuits: DEGRADED]
    
    E1 & R1 & C1 --> Healthy[Status: UP]
    E2 --> Down[Status: DOWN]
    R2 --> Down
    C2 --> Degraded[Status: UP<br/>with warnings]
    
    Healthy --> Response[200 OK]
    Degraded --> Response
    Down --> ErrorResponse[503 Service Unavailable]
    
    style Healthy fill:#4CAF50,color:#fff
    style Degraded fill:#FF9800,color:#fff
    style Down fill:#f44336,color:#fff
```

---

## 🎯 Resumen de Componentes

| Componente | Función | Tecnología |
|------------|---------|------------|
| **Routing** | Enrutamiento dinámico | Spring Cloud Gateway |
| **Service Discovery** | Descubrimiento de servicios | Netflix Eureka Client |
| **Circuit Breaker** | Tolerancia a fallos | Resilience4j |
| **Load Balancing** | Balanceo de carga | Ribbon (incluido en Eureka) |
| **CORS** | Gestión de CORS | Spring Cloud Gateway |
| **Timeouts** | Control de tiempos | Resilience4j TimeLimiter |

---

## 🔗 Referencias

- [README Principal](./README.md)
- [Configuración](./src/main/resources/application.yml)
- [Fallback Controller](./src/main/java/com/busconnect/apigateway/controller/FallbackController.java)
