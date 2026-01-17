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
    
    Client->>Gateway: POST /api/catalog/calculate<br/>{originId, destinationId}
    Gateway->>Catalog: POST /calculate
    
    Note over Catalog: 1. Validar municipios
    
    Catalog->>Cache: ¿Municipio origen en caché?
    Cache-->>Catalog: Miss
    Catalog->>DB: SELECT * FROM municipalities<br/>WHERE id = originId
    DB-->>Catalog: Municipality(lat, lon)
    Catalog->>Cache: Store municipio (24h)
    
    Catalog->>Cache: ¿Municipio destino en caché?
    Cache-->>Catalog: Hit ✓
    
    Note over Catalog: 2. Generar cache key<br/>origin_dest_driving
    
    Catalog->>Cache: ¿Ruta calculada?
    Cache-->>Catalog: Miss
    
    Note over Catalog: 3. Llamar OpenRouteService
    
    Catalog->>ORS: POST /v2/directions/driving-car<br/>coordinates: [[lon1,lat1],[lon2,lat2]]
    
    Note over ORS: Rate Limit Check<br/>2000 calls/day
    
    ORS-->>Catalog: {<br/>  distance: 85.5 km<br/>  duration: 3600 s<br/>  geometry: encoded<br/>}
    
    Note over Catalog: 4. Procesar respuesta
    
    Catalog->>Cache: Store ruta (1h TTL)
    Catalog->>DB: INSERT INTO routes
    
    Catalog-->>Gateway: 200 OK<br/>{route details}
    Gateway-->>Client: Route Response
```

---

## 🗄️ Sistema de Caché Multinivel

Estrategia de caché con Caffeine para optimizar rendimiento:

```mermaid
flowchart TB
    Request[Request: Calculate Route] --> ParseInput[Parse Input<br/>originId, destinationId]
    
    ParseInput --> CacheKey[Generate Cache Key<br/>origin_dest_profile]
    
    CacheKey --> RouteCache{Route Cache<br/>Caffeine}
    
    RouteCache -->|HIT ✓<br/>1h TTL| ReturnCached[Return Cached Route<br/>⚡ ~1ms]
    
    RouteCache -->|MISS| MuniCache1{Municipality Cache<br/>Origin}
    
    MuniCache1 -->|HIT ✓<br/>24h TTL| UseCached1[Use Cached Municipality]
    MuniCache1 -->|MISS| FetchDB1[Fetch from DB<br/>~10ms]
    FetchDB1 --> StoreCache1[Store in Cache]
    StoreCache1 --> UseCached1
    
    UseCached1 --> MuniCache2{Municipality Cache<br/>Destination}
    
    MuniCache2 -->|HIT ✓<br/>24h TTL| UseCached2[Use Cached Municipality]
    MuniCache2 -->|MISS| FetchDB2[Fetch from DB<br/>~10ms]
    FetchDB2 --> StoreCache2[Store in Cache]
    StoreCache2 --> UseCached2
    
    UseCached2 --> CallORS[Call OpenRouteService<br/>~500-2000ms]
    
    CallORS --> StoreRoute[Store Route in Cache<br/>1h TTL]
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

### Estrategia de Keys

```
Route Cache Key Format:
{originId}_{destinationId}_{profile}

Ejemplos:
"08001_08015_driving"    → Barcelona a Badalona (coche)
"08001_08015_cycling"    → Barcelona a Badalona (bici)
"08089_08245_driving"    → Manresa a Vic (coche)
```

---

## 🌐 Integración con OpenRouteService

Flujo de comunicación con la API externa:

```mermaid
flowchart TB
    Start[Catalog Service] --> RateCheck{Rate Limit Check<br/>2000/day}
    
    RateCheck -->|Límite alcanzado| RateLimitError[429 Too Many Requests<br/>Retry-After: 86400s]
    RateCheck -->|OK ✓| PrepareRequest[Preparar Request]
    
    PrepareRequest --> BuildBody[Build Request Body:<br/>coordinates: [[lon1,lat1], [lon2,lat2]]<br/>profile: driving-car<br/>format: geojson<br/>units: m]
    
    BuildBody --> AddHeaders[Add Headers:<br/>Authorization: Bearer API_KEY<br/>Content-Type: application/json<br/>Accept: application/json]
    
    AddHeaders --> SendRequest[POST<br/>https://api.openrouteservice.org<br/>/v2/directions/driving-car]
    
    SendRequest --> ORSProcess[OpenRouteService<br/>Processing]
    
    ORSProcess --> CheckResponse{Response Status}
    
    CheckResponse -->|200 OK| ParseSuccess[Parse Response:<br/>- distance (meters)<br/>- duration (seconds)<br/>- geometry (GeoJSON)<br/>- bbox (bounding box)]
    
    CheckResponse -->|400 Bad Request| ValidationError[Invalid Coordinates<br/>o parámetros]
    
    CheckResponse -->|401 Unauthorized| AuthError[API Key inválida]
    
    CheckResponse -->|404 Not Found| RouteNotFound[No route found<br/>entre puntos]
    
    CheckResponse -->|503 Service Unavailable| ServiceDown[ORS temporalmente<br/>no disponible]
    
    ParseSuccess --> Transform[Transform to Domain Model:<br/>Route entity]
    
    Transform --> CacheStore[Store in Cache<br/>1h TTL]
    CacheStore --> DBStore[Persist to DB]
    DBStore --> Success[Return Route]
    
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
    Start[Application Startup] --> Flyway[Flyway Migration<br/>V1__create_schema]
    
    Flyway --> CreateTable[CREATE TABLE municipalities]
    CreateTable --> LoadData[INSERT 947 municipios<br/>con coordenadas]
    
    LoadData --> Complete[✓ 947 municipios cargados]
    
    subgraph " "
        Complete --> Ready[Service Ready]
    end
    
    Ready --> SearchFlow[Search Flow]
    
    SearchFlow --> SearchType{Tipo de búsqueda}
    
    SearchType -->|GET /municipalities| GetAll[Listar todos<br/>947 municipios]
    SearchType -->|GET /municipalities/search| SearchByName[Buscar por nombre]
    
    GetAll --> CacheCheck1{En caché?}
    CacheCheck1 -->|Yes| ReturnCached1[Return from cache]
    CacheCheck1 -->|No| QueryDB1[SELECT * FROM municipalities<br/>ORDER BY name]
    QueryDB1 --> StoreCache1[Store 24h]
    StoreCache1 --> ReturnCached1
    
    SearchByName --> CacheCheck2{En caché?}
    CacheCheck2 -->|Yes| ReturnCached2[Return from cache]
    CacheCheck2 -->|No| QueryDB2[SELECT * WHERE<br/>name ILIKE '%query%']
    QueryDB2 --> StoreCache2[Store 24h]
    StoreCache2 --> ReturnCached2
    
    style Flyway fill:#4CAF50,stroke:#2E7D32,color:#fff
    style Complete fill:#2196F3,stroke:#1565C0,color:#fff
    style CacheCheck1 fill:#FF9800,stroke:#E65100,color:#fff
    style CacheCheck2 fill:#FF9800,stroke:#E65100,color:#fff
```

### Estructura de Datos

```
Municipality Entity:
├── id: Long (Primary Key)
├── name: String (e.g., "Barcelona")
├── province: String (e.g., "Barcelona")
├── latitude: Double (41.3851)
├── longitude: Double (2.1734)
├── postalCode: String (e.g., "08001")
└── comarca: String (e.g., "Barcelonès")

Indices:
├── PRIMARY KEY (id)
├── INDEX idx_name (name)
└── INDEX idx_coordinates (latitude, longitude)

Total registros: 947 municipios
Storage: ~150 KB en DB
Cache memory: ~500 KB (todos en memoria)
```

---

## 📊 Modelo de Datos Completo

Entidades y relaciones del servicio:

```mermaid
erDiagram
    ROUTE ||--o{ COMPANY : "operated_by"
    ROUTE ||--o{ SCHEDULE : "has"
    ROUTE }o--|| MUNICIPALITY : "origin"
    ROUTE }o--|| MUNICIPALITY : "destination"
    ROUTE ||--|| VEHICLE_TYPE : "uses"
    
    ROUTE {
        Long id PK
        Long originId FK
        Long destinationId FK
        String originName
        String destinationName
        Double distance
        Integer estimatedDuration
        String geometry
        LocalDateTime createdAt
    }
    
    MUNICIPALITY {
        Long id PK
        String name
        String province
        Double latitude
        Double longitude
        String postalCode
        String comarca
    }
    
    COMPANY {
        Long id PK
        String name
        String contactInfo
        Boolean isActive
    }
    
    SCHEDULE {
        Long id PK
        Long routeId FK
        LocalTime departureTime
        LocalTime arrivalTime
        String dayOfWeek
    }
    
    VEHICLE_TYPE {
        Long id PK
        String type
        Integer capacity
        Boolean hasAccessibility
    }
```

### Queries Principales

```sql
-- 1. Buscar ruta entre municipios
SELECT r.* FROM routes r
WHERE r.origin_id = ? AND r.destination_id = ?
ORDER BY r.created_at DESC LIMIT 1;

-- 2. Buscar municipio por nombre
SELECT * FROM municipalities
WHERE name ILIKE '%?%'
ORDER BY name;

-- 3. Rutas más frecuentes (analytics)
SELECT origin_name, destination_name, COUNT(*) as total
FROM routes
GROUP BY origin_name, destination_name
ORDER BY total DESC
LIMIT 10;

-- 4. Municipios sin rutas
SELECT m.* FROM municipalities m
LEFT JOIN routes r ON m.id = r.origin_id OR m.id = r.destination_id
WHERE r.id IS NULL;
```

---

## 🔄 Ciclo de Vida de una Ruta

Estados y procesamiento de una ruta calculada:

```mermaid
stateDiagram-v2
    [*] --> Requested: Client POST /calculate
    
    Requested --> Validating: Validar municipios
    
    Validating --> Invalid: Municipio no existe
    Validating --> CacheCheck: Municipios válidos ✓
    
    CacheCheck --> Cached: Ruta en caché (hit)
    CacheCheck --> Calculating: No en caché (miss)
    
    Calculating --> CallingAPI: Call OpenRouteService
    
    CallingAPI --> RateLimited: 429 Too Many Requests
    CallingAPI --> APIError: 4xx/5xx Error
    CallingAPI --> Calculated: 200 OK
    
    Calculated --> Storing: Guardar en DB
    Storing --> Caching: Guardar en caché (1h)
    Caching --> Completed: Return to client
    
    Cached --> Completed: Return cached data
    
    Completed --> [*]
    Invalid --> [*]
    RateLimited --> [*]
    APIError --> [*]
    
    note right of Cached
        ⚡ Fast path
        ~1-5ms response
    end note
    
    note right of Calculated
        🐌 Slow path
        ~500-2000ms response
    end note
```

---

## 🏗️ Arquitectura Reactiva

Stack técnico con Spring WebFlux:

```mermaid
flowchart LR
    Client[Client Request] --> Controller[RouteController<br/>@RestController]
    
    Controller --> Service[RouteService<br/>@Service]
    
    Service --> Cache[CacheManager<br/>Caffeine]
    Service --> Repo[RouteRepository<br/>R2DBC]
    Service --> ORS[OpenRouteService<br/>WebClient]
    
    Cache -.->|Mono/Flux| Service
    Repo -.->|Mono/Flux| Service
    ORS -.->|Mono| Service
    
    Service -.->|Mono| Controller
    Controller -.->|JSON| Client
    
    Repo --> R2DBC[R2DBC Pool<br/>PostgreSQL]
    R2DBC --> DB[(PostgreSQL<br/>catalog schema)]
    
    style Controller fill:#4CAF50,stroke:#2E7D32,color:#fff
    style Service fill:#2196F3,stroke:#1565C0,color:#fff
    style Cache fill:#FF9800,stroke:#E65100,color:#fff
    style ORS fill:#9C27B0,stroke:#6A1B9A,color:#fff
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
