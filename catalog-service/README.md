# Catalog Service - BusConnect

## 📋 Descripción

Microservicio reactivo para la gestión del catálogo de rutas de autobuses en Catalunya. Proporciona endpoints para calcular rutas entre municipios, buscar municipios, y gestionar información de rutas, horarios y compañías de transporte.

## 🏗️ Arquitectura

Este servicio implementa una arquitectura **totalmente reactiva** utilizando:

- **Spring WebFlux**: Framework reactivo no bloqueante
- **Spring Data R2DBC**: Acceso reactivo a PostgreSQL
- **Programación Reactiva**: Uso de Mono y Flux para procesamiento asíncrono
- **OpenRouteService API**: Cálculo de rutas geográficas
- **Caché Caffeine**: Optimización de rendimiento y reducción de llamadas a API externa

## 🔧 Tecnologías

- **Spring Boot**: 3.3.13
- **Spring WebFlux**: Programación reactiva
- **Spring Data R2DBC**: Base de datos reactiva
- **PostgreSQL**: Base de datos (driver R2DBC)
- **Flyway**: Migraciones de base de datos
- **Caffeine**: Caché en memoria de alto rendimiento
- **OpenRouteService API**: Servicio externo de cálculo de rutas
- **SpringDoc OpenAPI**: Documentación automática (Swagger)
- **Lombok**: Reducción de código boilerplate

## 📦 Configuración

### Variables de Entorno

| Variable | Descripción | Valor por Defecto | Requerida |
|----------|-------------|-------------------|-----------|
| `DB_HOST` | Host de PostgreSQL | `localhost` | ✅ |
| `DB_PORT` | Puerto de PostgreSQL | `5432` | ✅ |
| `DB_NAME` | Nombre de la base de datos | `busconnectdb` | ✅ |
| `DB_USERNAME` | Usuario de PostgreSQL | `busconnect_user` | ✅ |
| `DB_PASSWORD` | Contraseña de PostgreSQL | - | ✅ |
| `OPENROUTE_API_KEY` | API Key de OpenRouteService | - | ✅ |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | URL de Eureka Server | `http://localhost:8761/eureka/` | ⚠️ |

### Configuración del Caché

```yaml
cache:
  routes:
    maximum-size: 1000            # Máximo de rutas cacheadas
    expire-after-write-hours: 1   # Expiración en 1 hora
  municipalities:
    maximum-size: 1000            # Catalunya tiene 947 municipios
    expire-after-write-hours: 24  # Expiración en 24 horas
```

### Configuración de OpenRouteService

```yaml
openroute:
  api:
    key: ${OPENROUTE_API_KEY}
    base-url: https://api.openrouteservice.org
    timeout: 20s
    rate-limit:
      max-requests-per-day: 2000
```

### Puertos

- **Puerto del servicio**: `8083`
- **PostgreSQL**: `5432`
- **Eureka Server**: `8761`

## 🗄️ Modelo de Datos

### Entidades Principales

#### Municipality (Municipio)
```java
- id: Long
- name: String
- province: String (Barcelona, Girona, Lleida, Tarragona)
- latitude: Double
- longitude: Double
- population: Integer
- postalCode: String
```

#### Route (Ruta)
```java
- id: Long
- originId: Long (FK → Municipality)
- destinationId: Long (FK → Municipality)
- companyId: Long (FK → Company)
- duration: Integer (minutos)
- distance: Double (km)
- price: BigDecimal
- active: Boolean
```

#### Company (Compañía)
```java
- id: Long
- name: String
- contactEmail: String
- contactPhone: String
- website: String
- active: Boolean
```

#### Schedule (Horario)
```java
- id: Long
- routeId: Long (FK → Route)
- dayOfWeek: String
- departureTime: LocalTime
- arrivalTime: LocalTime
```

#### VehicleType (Tipo de Vehículo)
```java
- id: Long
- name: String
- capacity: Integer
- hasWifi: Boolean
- hasAC: Boolean
- hasToilet: Boolean
```

## 🚀 Endpoints API

### 📍 Cálculo de Rutas

#### POST `/api/routes/calculate`
Calcula una ruta entre dos municipios usando OpenRouteService.

**Request Body:**
```json
{
  "origin": "Barcelona",
  "destination": "Girona"
}
```

**Response:**
```json
{
  "origin": "Barcelona",
  "destination": "Girona",
  "distance": 103.2,
  "duration": 75.5,
  "estimatedPrice": 12.50,
  "cached": false,
  "coordinates": [
    [2.1734, 41.3851],
    [2.8214, 41.9794]
  ]
}
```

**Códigos de Estado:**
- `200 OK`: Ruta calculada exitosamente
- `400 Bad Request`: Datos de entrada inválidos
- `404 Not Found`: Municipio no encontrado
- `429 Too Many Requests`: Límite de API excedido
- `503 Service Unavailable`: OpenRouteService no disponible

### 🏘️ Gestión de Municipios

#### GET `/api/routes/municipalities`
Lista todos los municipios de Catalunya (947 municipios).

**Response:**
```json
[
  {
    "id": 1,
    "name": "Barcelona",
    "province": "Barcelona",
    "latitude": 41.3851,
    "longitude": 2.1734,
    "population": 1620343,
    "postalCode": "08001"
  }
]
```

#### GET `/api/routes/municipalities/search?name={name}`
Busca municipios por nombre (búsqueda parcial, case-insensitive).

**Parámetros:**
- `name`: Nombre o parte del nombre del municipio

**Ejemplo:**
```
GET /api/routes/municipalities/search?name=Barce
```

#### GET `/api/routes/municipalities/{province}`
Obtiene municipios filtrados por provincia.

**Provincias válidas:**
- `Barcelona`
- `Girona`
- `Lleida`
- `Tarragona`

**Ejemplo:**
```
GET /api/routes/municipalities/Barcelona
```

### 📊 Monitoreo y Estadísticas

#### GET `/api/routes/health`
Health check del servicio.

**Response:**
```json
{
  "status": "UP",
  "service": "catalog-service"
}
```

#### GET `/api/routes/rate-limit-stats`
Estadísticas de uso de la API de OpenRouteService.

**Response:**
```json
{
  "requestsToday": 150,
  "remainingRequests": 1850,
  "dailyLimit": 2000
}
```

#### GET `/api/routes/cache-stats`
Estadísticas del sistema de caché.

**Response:**
```json
{
  "routeCache": {
    "size": 45,
    "hitRate": 0.78,
    "missRate": 0.22
  },
  "municipalityCache": {
    "size": 947,
    "hitRate": 0.95,
    "missRate": 0.05
  }
}
```

### 🌊 Streaming Reactivo

#### GET `/api/routes/stream`
Stream reactivo de múltiples cálculos de rutas (máximo 20).

**Response:** Server-Sent Events (SSE)
```
Content-Type: text/event-stream

data: {"origin":"Barcelona","destination":"Girona",...}
data: {"origin":"Barcelona","destination":"Lleida",...}
```

## 📚 Documentación API (Swagger)

Acceder a la documentación interactiva:

- **Swagger UI**: `http://localhost:8083/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8083/api-docs`

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
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Construcción de la imagen

```bash
docker build -t busconnect/catalog-service:latest .
```

### Ejecución con Docker

```bash
docker run -d \
  -p 8083:8083 \
  -e DB_HOST=postgres \
  -e DB_PORT=5432 \
  -e DB_NAME=busconnectdb \
  -e DB_USERNAME=busconnect_user \
  -e DB_PASSWORD=your_password \
  -e OPENROUTE_API_KEY=your_api_key \
  --name catalog-service \
  busconnect/catalog-service:latest
```

## 🚀 Ejecución

### Desarrollo Local

**Requisitos previos:**
- Java 21
- Maven 3.9+
- PostgreSQL 15+
- OpenRouteService API Key

**Pasos:**

1. **Configurar variables de entorno** (crear `.env`):
```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=busconnectdb
DB_USERNAME=busconnect_user
DB_PASSWORD=your_password
OPENROUTE_API_KEY=your_api_key
```

2. **Ejecutar con Maven**:
```bash
mvn spring-boot:run
```

3. **O compilar y ejecutar JAR**:
```bash
mvn clean package
java -jar target/catalog-service-0.0.1-SNAPSHOT.jar
```

### Con Docker Compose

```bash
docker-compose up catalog-service
```

## 💾 Base de Datos

### Esquema

El servicio utiliza el esquema `catalog` en PostgreSQL.

### Migraciones Flyway

Las migraciones se encuentran en: `src/main/resources/db/migration/`

**V1__create_schema_and_municipalities.sql**:
- Crea el esquema `catalog`
- Crea la tabla `municipalities`
- Inserta los 947 municipios de Catalunya con coordenadas

**Próximas migraciones**:
- V2: Tablas `companies`, `routes`, `schedules`, `vehicle_types`

### Población de Datos Iniciales

Los 947 municipios de Catalunya se cargan automáticamente en el primer arranque mediante Flyway.

## ⚡ Optimización y Rendimiento

### Sistema de Caché

El servicio implementa caché en dos niveles:

1. **Caché de Rutas** (1 hora):
   - Reduce llamadas a OpenRouteService API
   - Ahorro económico significativo
   - Compartido entre todos los usuarios

2. **Caché de Municipios** (24 horas):
   - Evita consultas frecuentes a BD
   - Los municipios rara vez cambian
   - 947 municipios precargados

### Programación Reactiva

Ventajas del stack reactivo:

- ✅ **No bloqueante**: Manejo eficiente de I/O
- ✅ **Escalable**: Más peticiones con menos recursos
- ✅ **Backpressure**: Control de flujo automático
- ✅ **Composición**: Operadores funcionales (map, filter, flatMap)

### Rate Limiting

Control de uso de OpenRouteService API:

- **Límite diario**: 2000 peticiones
- **Contador interno**: Seguimiento de uso
- **Respuesta 429**: Cuando se excede el límite
- **Caché**: Reduce llamadas repetidas

## 🧪 Pruebas

### Health Check

```bash
curl http://localhost:8083/api/routes/health
```

### Calcular Ruta

```bash
curl -X POST http://localhost:8083/api/routes/calculate \
  -H "Content-Type: application/json" \
  -d '{
    "origin": "Barcelona",
    "destination": "Girona"
  }'
```

### Buscar Municipios

```bash
# Todos los municipios
curl http://localhost:8083/api/routes/municipalities

# Por nombre
curl http://localhost:8083/api/routes/municipalities/search?name=Barce

# Por provincia
curl http://localhost:8083/api/routes/municipalities/Barcelona
```

### Ver Estadísticas

```bash
# Rate limit
curl http://localhost:8083/api/routes/rate-limit-stats

# Caché
curl http://localhost:8083/api/routes/cache-stats
```

## 📝 Estructura del Proyecto

```
catalog-service/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/busconnect/catalogservice/
│       │       ├── CatalogServiceApplication.java
│       │       ├── config/
│       │       │   └── OpenRouteProperties.java
│       │       ├── controller/
│       │       │   └── RouteController.java
│       │       ├── dto/
│       │       │   ├── request/
│       │       │   │   └── CalculateRouteRequest.java
│       │       │   └── response/
│       │       │       ├── RouteResultResponse.java
│       │       │       ├── ErrorResponse.java
│       │       │       └── ValidationErrorResponse.java
│       │       ├── exception/
│       │       │   ├── GlobalExceptionHandler.java
│       │       │   ├── MunicipalityNotFoundException.java
│       │       │   ├── OpenRouteServiceException.java
│       │       │   └── RateLimitExceededException.java
│       │       ├── model/
│       │       │   ├── Municipality.java
│       │       │   ├── Route.java
│       │       │   ├── Company.java
│       │       │   ├── Schedule.java
│       │       │   └── VehicleType.java
│       │       ├── repository/
│       │       │   ├── MunicipalityRepository.java
│       │       │   ├── RouteRepository.java
│       │       │   ├── CompanyRepository.java
│       │       │   ├── ScheduleRepository.java
│       │       │   └── VehicleTypeRepository.java
│       │       └── service/
│       │           ├── MunicipalityService.java
│       │           └── OpenRouteService.java
│       └── resources/
│           ├── application.yml
│           ├── messages.properties
│           └── db/
│               └── migration/
│                   └── V1__create_schema_and_municipalities.sql
├── .dockerignore
├── .env.example
├── Dockerfile
├── pom.xml
└── README.md
```

## 🔐 Seguridad

### Buenas Prácticas Implementadas

- ✅ Variables de entorno para credenciales sensibles
- ✅ `.env` excluido de Git y Docker
- ✅ Validación de entrada con Jakarta Validation
- ✅ Manejo centralizado de excepciones
- ✅ Logging sin información sensible
- ✅ Rate limiting en API externa
- ✅ Timeout configurado (20s) para evitar bloqueos

### Próximas Mejoras de Seguridad

- [ ] Autenticación JWT
- [ ] Rate limiting por usuario
- [ ] Encriptación de datos sensibles
- [ ] Auditoría de accesos
- [ ] HTTPS en producción

## 🐛 Troubleshooting

### Error: "Municipality not found"

**Causa**: El municipio no existe en la base de datos.

**Solución**: 
- Verificar nombre exacto con `/municipalities/search?name=`
- Los municipios deben existir en Catalunya

### Error: "Rate limit exceeded"

**Causa**: Se alcanzó el límite de 2000 peticiones/día de OpenRouteService.

**Solución**:
- Esperar hasta el próximo día
- Aumentar el límite en OpenRouteService (plan de pago)
- Las rutas cacheadas no consumen cuota

### Error: Conexión a PostgreSQL

**Causa**: PostgreSQL no está disponible o credenciales incorrectas.

**Solución**:
```bash
# Verificar que PostgreSQL esté corriendo
docker ps | grep postgres

# Verificar variables de entorno
echo $DB_HOST $DB_USERNAME

# Verificar conectividad
psql -h $DB_HOST -U $DB_USERNAME -d $DB_NAME
```

### Flyway Baseline Error

**Causa**: Base de datos ya tiene el esquema pero sin control de Flyway.

**Solución**: Configurado `baseline-on-migrate: true` automáticamente.

### Caché no funciona

**Causa**: Configuración incorrecta o caché deshabilitado.

**Verificar**:
```bash
# Ver estadísticas del caché
curl http://localhost:8083/api/routes/cache-stats
```

## 📊 Monitoreo

### Logs

```bash
# Ver logs en Docker
docker logs -f catalog-service

# Nivel de logs configurado
logging:
  level:
    com.busconnect.catalogservice: DEBUG
    org.springframework.data.r2dbc: DEBUG
    io.r2dbc.postgresql: INFO
```

### Métricas

El servicio expone métricas básicas a través de los endpoints:
- Health: `/api/routes/health`
- Rate limit: `/api/routes/rate-limit-stats`
- Cache: `/api/routes/cache-stats`

## 🔄 Integración con Otros Servicios

### Service Discovery (Eureka)

El servicio se registra automáticamente en Eureka Server:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://eureka-service:8761/eureka/
  instance:
    prefer-ip-address: true
```

### API Gateway

Las peticiones pasan por el API Gateway:

```
Cliente → API Gateway (8080) → Catalog Service (8083)
```

**Ruta**: `/api/catalog/**` → `catalog-service`

## 📞 APIs Externas

### OpenRouteService

- **Proveedor**: OpenRouteService.org
- **Propósito**: Cálculo de rutas geográficas
- **Límite**: 2000 peticiones/día (plan gratuito)
- **Documentación**: https://openrouteservice.org/dev/#/api-docs

**Registro**:
1. Crear cuenta en https://openrouteservice.org
2. Generar API Key
3. Configurar en `.env`: `OPENROUTE_API_KEY=your_key`

## 📄 Licencia

Este proyecto es parte del sistema BusConnect.

## 🤝 Contribución

Para contribuir al servicio, consultar el [README principal](../README.md) y seguir las guías de estilo del proyecto.

## 📚 Recursos Adicionales

- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
- [Spring Data R2DBC](https://spring.io/projects/spring-data-r2dbc)
- [Project Reactor](https://projectreactor.io/)
- [OpenRouteService API](https://openrouteservice.org/dev/#/api-docs)
- [Caffeine Cache](https://github.com/ben-manes/caffeine)
