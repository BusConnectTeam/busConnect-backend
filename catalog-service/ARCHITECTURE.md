# Catalog Service - Arquitectura

## 🚌 Flujo de Cálculo de Rutas

Proceso completo desde la petición hasta la respuesta con ruta optimizada:

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway
    participant Catalog as Catalog Service
    participant Cache as Caffeine Cache
    participant DB as PostgreSQL
    participant ORS as OpenRouteService API
    
    Client->>Gateway: POST /api/routes/calculate<br/>Request body with municipalities
    Gateway->>Catalog: POST /routes/calculate
    
    Note over Catalog: 1. Validar municipios
    
    Catalog->>Cache: Check municipio origen en cache?
    Cache-->>Catalog: Miss
    Catalog->>DB: SELECT * FROM municipalities<br/>WHERE name ILIKE originName
    DB-->>Catalog: Municipality data with lat/lon
    Catalog->>Cache: Store municipio 24h
    
    Catalog->>Cache: Check municipio destino en cache?
    Cache-->>Catalog: Hit OK
    
    Note over Catalog: 2. Generar cache key<br/>origin_dest_driving
    
    Catalog->>Cache: Check ruta calculada?
    Cache-->>Catalog: Miss
    
    Note over Catalog: 3. Llamar OpenRouteService
    
    Catalog->>ORS: GET /v2/directions/driving-car<br/>?start=lon1,lat1&end=lon2,lat2
    
    Note over ORS: Rate Limit Check<br/>2000 calls/day
    
    ORS-->>Catalog: Response:<br/>distance 85.5 km duration 3600 s<br/>geometry encoded
    
    Note over Catalog: 4. Procesar respuesta
    
    Catalog->>Cache: Store ruta (1h TTL)
    
    Catalog-->>Gateway: 200 OK<br/>{route details}
    Gateway-->>Client: Route Response
```

---

## 🗄️ Sistema de Caché Multinivel

Estrategia de caché con Caffeine para optimizar rendimiento:

```mermaid
flowchart TB
    Request[Request: Calculate Route] --> ParseInput[Parse Input municipalities]
    
    ParseInput --> CacheKey[Generate Cache Key<br/>origin-destination]
    
    CacheKey --> RouteCache{Route Cache<br/>Caffeine}
    
    RouteCache -->|HIT - 1h TTL| ReturnCached[Return Cached Route - ~1ms]
    
    RouteCache -->|MISS| MuniCache1{Municipality Cache Origin}
    
    MuniCache1 -->|HIT - 24h TTL| UseCached1[Use Cached Municipality]
    MuniCache1 -->|MISS| FetchDB1[Fetch from DB ~10ms]
    FetchDB1 --> StoreCache1[Store in Cache]
    StoreCache1 --> UseCached1
    
    UseCached1 --> MuniCache2{Municipality Cache Destination}
    
    MuniCache2 -->|HIT - 24h TTL| UseCached2[Use Cached Municipality]
    MuniCache2 -->|MISS| FetchDB2[Fetch from DB ~10ms]
    FetchDB2 --> StoreCache2[Store in Cache]
    StoreCache2 --> UseCached2
    
    UseCached2 --> CallORS[Call OpenRouteService ~500-2000ms]
    
    CallORS --> StoreRoute[Store Route in Cache 1h TTL]
    StoreRoute --> SaveDB[Save to DB]
    SaveDB --> ReturnNew[Return New Route]
    
    ReturnCached --> End[Response]
    ReturnNew --> End
    
    style RouteCache fill:#4CAF50,stroke:#2E7D32,color:#fff
    style MuniCache1 fill:#2196F3,stroke:#1565C0,color:#fff
    style MuniCache2 fill:#2196F3,stroke:#1565C0,color:#fff
    style ReturnCached fill:#FFEB3B,stroke:#F57F17
    style CallORS fill:#FF5722,stroke:#BF360C,color:#fff
```

### Configuración de Caché

| Caché | Max Entries | TTL | Hit Rate Esperado | Tiempo Respuesta |
|-------|-------------|-----|-------------------|------------------|
| **Routes** | 1000 | 1 hora | ~70-80% | ~1ms (cached) |
| **Municipalities** | 1000 | 24 horas | ~95%+ | ~1ms (cached) |
| **DB Query** | - | - | - | ~10-50ms |
| **OpenRouteService** | - | - | - | ~500-2000ms |

**Nota**: Las rutas calculadas NO se persisten en base de datos. Son efímeras y solo viven en caché durante 1 hora. Esto evita almacenamiento innecesario ya que las rutas pueden recalcularse bajo demanda.

### Estrategia de Keys

```
Route Cache Key Format:
{origin}-{destination}

Ejemplos:
"barcelona-badalona"    → Barcelona a Badalona
"barcelona-girona"      → Barcelona a Girona
"manresa-vic"           → Manresa a Vic

Nota: Los nombres se normalizan a minúsculas y se eliminan espacios
```

---

## 🌐 Integración con OpenRouteService

Flujo de comunicación con la API externa:

```mermaid
flowchart TB
    Start[Catalog Service] --> RateCheck{Rate Limit Check 2000/day}
    
    RateCheck -->|Límite alcanzado| RateLimitError[429 Too Many Requests]
    RateCheck -->|OK ✓| PrepareRequest[Preparar Request]
    
    PrepareRequest --> BuildURL[Build Request URL<br/>with coordinates]
    
    BuildURL --> SendRequest[GET OpenRouteService API]
    
    SendRequest --> ORSProcess[OpenRouteService Processing]
    
    ORSProcess --> CheckResponse{Response Status}
    
    CheckResponse -->|200 OK| ParseSuccess[Parse Response:<br/>distance, duration, geometry]
    
    CheckResponse -->|400 Bad Request| ValidationError[Invalid Coordinates]
    
    CheckResponse -->|401 Unauthorized| AuthError[API Key invalida]
    
    CheckResponse -->|404 Not Found| RouteNotFound[No route found]
    
    CheckResponse -->|503 Service Unavailable| ServiceDown[ORS temporarily unavailable]
    
    ParseSuccess --> CacheStore[Store in Cache 1h TTL]
    CacheStore --> Success[Return Route Response]
    
    ValidationError --> ErrorResponse[Error Response]
    AuthError --> ErrorResponse
    RouteNotFound --> ErrorResponse
    ServiceDown --> ErrorResponse
    RateLimitError --> ErrorResponse
    
    ErrorResponse --> End[Client receives error]
    Success --> End2[Client receives route]
    
    style RateCheck fill:#FF9800,stroke:#E65100,color:#fff
    style RateLimitError fill:#f44336,stroke:#c62828,color:#fff
    style ORSProcess fill:#2196F3,stroke:#1565C0,color:#fff
    style ParseSuccess fill:#4CAF50,stroke:#2E7D32,color:#fff
    style CacheStore fill:#9C27B0,stroke:#6A1B9A,color:#fff
```

### Rate Limiting

```
Daily Limit: 2000 calls
Reset: Cada día a las 00:00 UTC

Estrategia:
┌─────────────────────────────────┐
│ Cache Hit (1h) → No consume API │
│ Cache Miss → Consume 1 call     │
└─────────────────────────────────┘

Con 70% hit rate:
- 10,000 requests/day
- 3,000 API calls needed
- ❌ Excede límite

Con caché necesitamos:
- Hit rate > 80% para <2000 calls/day
```

---

## 🗺️ Gestión de Municipios

Arquitectura de datos para 947 municipios de Catalunya:

```mermaid
flowchart TB
    Start[Application Startup] --> Flyway[Flyway Migration V1]
    
    Flyway --> CreateTable[CREATE TABLE municipalities]
    CreateTable --> LoadData[INSERT 947 municipios]
    
    LoadData --> Complete[947 municipios cargados OK]
    
    subgraph " "
        Complete --> Ready[Service Ready]
    end
    
    Ready --> SearchFlow[Search Flow]
    
    SearchFlow --> SearchType{Tipo de búsqueda}
    
    SearchType -->|GET /municipalities| GetAll[Listar todos<br/>947 municipios]
    SearchType -->|GET /municipalities/search| SearchByName[Buscar por nombre]
    SearchType -->|GET by province| SearchByProvince[Buscar por provincia]
    
    GetAll --> CacheCheck1{En cache?}
    CacheCheck1 -->|Yes| ReturnCached1[Return from cache]
    CacheCheck1 -->|No| QueryDB1[SELECT all ORDER BY name]
    QueryDB1 --> StoreCache1[Store 24h]
    StoreCache1 --> ReturnCached1
    
    SearchByName --> CacheCheck2{En cache?}
    CacheCheck2 -->|Yes| ReturnCached2[Return from cache]
    CacheCheck2 -->|No| QueryDB2[SELECT WHERE name ILIKE query]
    QueryDB2 --> StoreCache2[Store 24h]
    StoreCache2 --> ReturnCached2
    
    SearchByProvince --> CacheCheck3{En cache?}
    CacheCheck3 -->|Yes| ReturnCached3[Return from cache]
    CacheCheck3 -->|No| QueryDB3[SELECT WHERE province equals]
    QueryDB3 --> StoreCache3[Store 24h]
    StoreCache3 --> ReturnCached3
    
    style Flyway fill:#4CAF50,stroke:#2E7D32,color:#fff
    style CacheCheck3 fill:#FF9800,stroke:#E65100,color:#fff
    style Complete fill:#2196F3,stroke:#1565C0,color:#fff
    style CacheCheck1 fill:#FF9800,stroke:#E65100,color:#fff
    style CacheCheck2 fill:#FF9800,stroke:#E65100,color:#fff
```

### Estructura de Datos

```
Municipality Entity:
├── id: UUID (Primary Key)
├── name: String (e.g., "Barcelona")
├── normalizedName: String (e.g., "barcelona")
├── province: String (e.g., "Barcelona", "Girona", "Lleida", "Tarragona")
├── latitude: BigDecimal (41.3874)
├── longitude: BigDecimal (2.1686)
├── postalCodes: String (e.g., "08001, 08002")
├── isActive: Boolean (default: true)
├── createdAt: LocalDateTime
└── updatedAt: LocalDateTime

Indices:
├── PRIMARY KEY (id)
├── INDEX idx_name (name)
├── INDEX idx_normalized (normalized_name)
├── INDEX idx_province (province)
├── INDEX idx_active (is_active)
└── INDEX idx_coordinates (latitude, longitude)

Total registros: 947 municipios
- Barcelona: 311 municipios
- Girona: 221 municipios  
- Lleida: 231 municipios
- Tarragona: 184 municipios

Storage: ~200 KB en DB
Cache memory: ~600 KB (todos en memoria)
```

---

## 📊 Modelo de Datos Completo

Entidades principales del servicio:

```mermaid
erDiagram
    MUNICIPALITY ||--o{ BUS_COMPANY : "operates_in"
    BUS_COMPANY ||--o{ DRIVER : "employs"
    BUS_COMPANY ||--o{ BUS_TYPE : "owns"
    
    MUNICIPALITY {
        UUID id PK
        String name
        String normalizedName
        String province
        BigDecimal latitude
        BigDecimal longitude
        String postalCodes
        Boolean isActive
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    BUS_COMPANY {
        UUID id PK
        String name
        String legalName
        String cif
        String email
        String phone
        String address
        String city
        String postalCode
        String website
        String logoUrl
        Integer foundedYear
        Boolean isActive
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    DRIVER {
        UUID id PK
        UUID companyId FK
        String firstName
        String lastName
        String dni
        String email
        String phone
        LocalDate birthDate
        LocalDate hireDate
        String licenseNumber
        LocalDate licenseExpiryDate
        String licenseType
        Integer yearsExperience
        String languages
        String photoUrl
        Boolean isActive
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    BUS_TYPE {
        UUID id PK
        UUID companyId FK
        String name
        Integer capacity
        Boolean hasWifi
        Boolean hasAc
        Boolean hasUsbChargers
        Boolean hasToilet
        Boolean hasWheelchairAccess
        Boolean hasLuggageCompartment
        Boolean hasEntertainmentSystem
        String seatType
        String description
        BigDecimal pricePerKm
        Boolean isActive
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
```

### Queries Principales

```sql
-- 1. Buscar municipio por nombre (case-insensitive)
SELECT * FROM catalog.municipalities
WHERE LOWER(name) = LOWER(?)
AND is_active = true;

-- 2. Buscar municipios por nombre parcial
SELECT * FROM catalog.municipalities
WHERE name ILIKE CONCAT('%', ?, '%')
AND is_active = true
ORDER BY name;

-- 3. Obtener municipios por provincia
SELECT * FROM catalog.municipalities
WHERE province = ?
AND is_active = true
ORDER BY name;

-- 4. Buscar empresas activas
SELECT * FROM catalog.bus_companies
WHERE is_active = true
ORDER BY name;

-- 5. Conductores de una empresa
SELECT * FROM catalog.drivers
WHERE company_id = ?
AND is_active = true
ORDER BY last_name, first_name;

-- 6. Tipos de autobuses con características específicas
SELECT bt.*, bc.name as company_name
FROM catalog.bus_types bt
JOIN catalog.bus_companies bc ON bt.company_id = bc.id
WHERE bt.has_wifi = true
AND bt.has_wheelchair_access = true
AND bt.is_active = true
AND bc.is_active = true;
```

---

## 🔄 Ciclo de Vida de un Cálculo de Ruta

Estados y procesamiento de una ruta calculada:

```mermaid
stateDiagram-v2
    [*] --> Requested: Client POST /routes/calculate
    
    Requested --> Validating: Validar municipios
    
    Validating --> Invalid: Municipio no existe
    Validating --> CacheCheck: Municipios validos OK
    
    CacheCheck --> Cached: Ruta en caché (hit)
    CacheCheck --> Calculating: No en caché (miss)
    
    Calculating --> CallingAPI: Call OpenRouteService
    
    CallingAPI --> RateLimited: 429 Too Many Requests
    CallingAPI --> APIError: 4xx/5xx Error
    CallingAPI --> Calculated: 200 OK
    
    Calculated --> Caching: Guardar en caché (1h)
    Caching --> Completed: Return to client
    
    Cached --> Completed: Return cached data
    
    Completed --> [*]
    Invalid --> [*]
    RateLimited --> [*]
    APIError --> [*]
    
    note right of Cached
        Fast path
        1-5ms response
        desde cache
    end note
    
    note right of Calculated
        Slow path  
        500-2000ms response
        llamada API externa
    end note
```

---

## 🏗️ Arquitectura Reactiva

Stack técnico con Spring WebFlux:

```mermaid
flowchart TB
    Client[Client Request] --> Controller[RouteController]
    
    Controller --> ORS[OpenRouteService]
    Controller --> MuniService[MunicipalityService]
    
    ORS --> Cache[CacheManager Caffeine]
    ORS --> MuniRepo[MunicipalityRepository R2DBC]
    ORS --> WebClient[WebClient API]
    
    MuniService --> MuniRepo
    MuniService --> Cache
    
    Cache -.->|Mono/Flux| ORS
    MuniRepo -.->|Mono/Flux| ORS
    MuniRepo -.->|Mono/Flux| MuniService
    WebClient -.->|Mono| ORS
    
    ORS -.->|Mono RouteResultResponse| Controller
    MuniService -.->|Flux Municipality| Controller
    Controller -.->|JSON| Client
    
    MuniRepo --> R2DBC[R2DBC Pool PostgreSQL]
    R2DBC --> DB[(PostgreSQL catalog)]
    
    style Controller fill:#4CAF50,stroke:#2E7D32,color:#fff
    style ORS fill:#2196F3,stroke:#1565C0,color:#fff
    style MuniService fill:#9C27B0,stroke:#6A1B9A,color:#fff
    style Cache fill:#FF9800,stroke:#E65100,color:#fff
    style WebClient fill:#E91E63,stroke:#880E4F,color:#fff
    style DB fill:#00BCD4,stroke:#006064,color:#fff
```

**Ventajas del Stack Reactivo:**
- Non-blocking I/O
- Mejor uso de threads (event loop)
- Backpressure automático
- Composición con operadores Mono/Flux
- Escalabilidad sin aumentar threads

---

## 🎯 Métricas y Monitoreo

KPIs del servicio:

```mermaid
flowchart TB
    Metrics[Actuator Metrics<br/>/actuator/metrics] --> Health[Health Indicators]
    
    Health --> DB_Health[DB Connection<br/>status: UP/DOWN]
    Health --> Cache_Health[Cache Stats<br/>hit rate, size]
    Health --> API_Health[ORS API<br/>available/unavailable]
    
    Metrics --> Performance[Performance Metrics]
    
    Performance --> ResponseTime[Response Time<br/>p50, p95, p99]
    Performance --> Throughput[Throughput<br/>requests/second]
    Performance --> ErrorRate[Error Rate<br/>4xx, 5xx]
    
    Metrics --> Business[Business Metrics]
    
    Business --> RoutesCalculated[Total Routes<br/>calculated]
    Business --> CacheHitRate[Cache Hit Rate<br/>target: >80%]
    Business --> APICallsUsed[API Calls Used<br/>limit: 2000/day]
    
    style DB_Health fill:#4CAF50,stroke:#2E7D32,color:#fff
    style Cache_Health fill:#2196F3,stroke:#1565C0,color:#fff
    style API_Health fill:#FF9800,stroke:#E65100,color:#fff
```

### Alertas Recomendadas

| Métrica | Umbral | Acción |
|---------|--------|--------|
| Cache Hit Rate | < 70% | Revisar TTL, aumentar size |
| API Calls Remaining | < 200/day | Activar rate limiting |
| Response Time p95 | > 3s | Revisar ORS, DB queries |
| Error Rate | > 5% | Check logs, ORS status |
| DB Connection Pool | > 80% used | Aumentar pool size |

---

## 🔗 Referencias

- [README Principal](./README.md)
- [Configuración](./src/main/resources/application.yml)
- [OpenRouteService API Docs](https://openrouteservice.org/dev/#/api-docs)
- [Flyway Migrations](./src/main/resources/db/migration/)
- [Route Controller](./src/main/java/com/busconnect/catalogservice/controller/RouteController.java)
