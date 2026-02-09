# API Gateway - BusConnect
## Documento Técnico

---

## 1. Resumen Técnico

### 1.1 Qué es este API Gateway
API Gateway es el punto de entrada único (single entry point) para todos los microservicios del ecosistema BusConnect. Implementado con Spring Cloud Gateway sobre una arquitectura reactiva basada en Spring WebFlux.

### 1.2 Rol exacto dentro de BusConnect
- **Proxy reverso centralizado**: Todas las peticiones HTTP externas ingresan al sistema a través del puerto `8080` del gateway
- **Enrutador dinámico**: Distribuye peticiones hacia microservicios backend basándose en patrones de URL
- **Balanceador de carga**: Utiliza Netflix Eureka para descubrimiento de servicios y distribución de tráfico
- **Mecanismo de resiliencia**: Implementa Circuit Breaker con Resilience4j para tolerancia a fallos
- **Gestión de políticas transversales**: CORS, headers de correlación, logging de peticiones

---

## 2. Alcance Funcional

### 2.1 Responsabilidades (qué hace)
- Enrutamiento de peticiones hacia `CATALOG-SERVICE` y `USER-SERVICE`
- Descubrimiento dinámico de instancias de microservicios vía Eureka
- Aplicación de filtros globales a todas las peticiones (Correlation ID, logging)
- Gestión de Circuit Breaker por servicio destino
- Configuración global de CORS para clientes web
- Exposición de endpoints de monitoreo (Actuator)
- Respuestas de fallback cuando servicios backend no están disponibles
- Deduplicación de headers de respuesta

### 2.2 Fuera de alcance (qué NO hace)
- **NO implementa lógica de negocio**: No procesa datos de dominio (usuarios, rutas, empresas)
- **NO persiste datos**: No tiene base de datos ni capa de persistencia
- **NO realiza autenticación/autorización**: No hay implementación de seguridad JWT/OAuth2 (detectado como limitación actual)
- **NO transforma payloads**: No modifica el cuerpo de peticiones/respuestas
- **NO implementa rate limiting**: No hay control de tasa de peticiones por cliente
- **NO gestiona sesiones**: Es completamente stateless
- **NO realiza caché**: No hay capa de almacenamiento temporal de respuestas

---

## 3. Estructura del Proyecto

### 3.1 Árbol de directorios
```
api-gateway/
├── src/main/
│   ├── java/com/busconnect/apigateway/
│   │   ├── ApiGatewayApplication.java          # Punto de entrada
│   │   ├── config/
│   │   │   └── GatewayConfig.java              # Filtros globales
│   │   └── controller/
│   │       └── FallbackController.java         # Endpoints de fallback
│   └── resources/
│       └── application.yml                     # Configuración principal
├── target/                                     # Artefactos compilados
├── Dockerfile                                  # Imagen Docker multi-stage
├── pom.xml                                     # Dependencias Maven
├── ARCHITECTURE.md                             # Diagramas de arquitectura
└── README.md                                   # Documentación funcional
```

### 3.2 Componentes principales

#### ApiGatewayApplication.java
**Ubicación**: [src/main/java/com/busconnect/apigateway/ApiGatewayApplication.java](src/main/java/com/busconnect/apigateway/ApiGatewayApplication.java)

**Rol**: Clase bootstrap de Spring Boot.

**Anotaciones relevantes**:
- `@SpringBootApplication`: Configuración auto de Spring Boot
- `@EnableDiscoveryClient`: Habilita registro y descubrimiento en Eureka

**Contenido**:
```java
public static void main(String[] args) {
    SpringApplication.run(ApiGatewayApplication.class, args);
}
```

#### GatewayConfig.java
**Ubicación**: [src/main/java/com/busconnect/apigateway/config/GatewayConfig.java](src/main/java/com/busconnect/apigateway/config/GatewayConfig.java)

**Rol**: Define filtros globales que se aplican a todas las peticiones.

**Beans declarados**:
1. **`requestTracingFilter()`** (Order: -1)
   - Genera o propaga `X-Correlation-ID` en headers
   - Añade el header a la respuesta

2. **`loggingFilter()`** (Order: 0)
   - Registra método HTTP, path, status code y duración de cada petición
   - Formato: `[GATEWAY] {METHOD} {PATH} -> {STATUS} ({DURATION}ms)`

#### FallbackController.java
**Ubicación**: [src/main/java/com/busconnect/apigateway/controller/FallbackController.java](src/main/java/com/busconnect/apigateway/controller/FallbackController.java)

**Rol**: Expone endpoints REST que actúan como fallback cuando Circuit Breaker está abierto.

**Endpoints**:
- `GET /fallback/catalog` → Retorna error 503 para `catalog-service`
- `GET /fallback/users` → Retorna error 503 para `user-service`

**Respuesta estándar**:
```json
{
  "status": "error",
  "service": "catalog-service",
  "message": "Catalog service is temporarily unavailable. Please try again later.",
  "timestamp": "2026-02-07T10:30:45"
}
```

#### application.yml
**Ubicación**: [src/main/resources/application.yml](src/main/resources/application.yml)

**Rol**: Configuración centralizada de routing, CORS, Eureka, Circuit Breaker y logging.

**Secciones clave**:
- `spring.cloud.gateway.routes`: Definición de rutas a microservicios
- `spring.cloud.gateway.globalcors`: Políticas CORS
- `eureka.client`: Configuración de Eureka Client
- `resilience4j.circuitbreaker`: Parámetros de Circuit Breaker
- `management.endpoints`: Actuator endpoints

#### Dockerfile
**Ubicación**: [Dockerfile](Dockerfile)

**Rol**: Define imagen Docker optimizada con multi-stage build.

**Stages**:
1. **dependencies**: Descarga dependencias Maven (cacheado)
2. **builder**: Compila aplicación y extrae layers de Spring Boot
3. **runtime**: Imagen JRE mínima con optimizaciones JVM

---

## 4. Entradas al Sistema

### 4.1 Rutas configuradas

#### Catalog Service - Routes
**Pattern**: `/api/routes/**`
**Target**: `lb://CATALOG-SERVICE`
**Métodos**: GET, POST, PUT, DELETE
**Circuit Breaker**: `catalogServiceCircuitBreaker`
**Fallback**: `forward:/fallback/catalog`
**Clase responsable**: Spring Cloud Gateway (routing automático)
**Definición**: [application.yml:17-25](src/main/resources/application.yml#L17-L25)

#### Catalog Service - Buses
**Pattern**: `/api/companies/buses/**`
**Target**: `lb://CATALOG-SERVICE`
**Métodos**: GET, POST, PUT, DELETE
**Circuit Breaker**: `catalogServiceCircuitBreaker`
**Fallback**: `forward:/fallback/catalog`
**Clase responsable**: Spring Cloud Gateway
**Definición**: [application.yml:28-36](src/main/resources/application.yml#L28-L36)

#### Catalog Service - Drivers
**Pattern**: `/api/companies/drivers/**`
**Target**: `lb://CATALOG-SERVICE`
**Métodos**: GET, POST, PUT, DELETE
**Circuit Breaker**: `catalogServiceCircuitBreaker`
**Fallback**: `forward:/fallback/catalog`
**Clase responsable**: Spring Cloud Gateway
**Definición**: [application.yml:39-47](src/main/resources/application.yml#L39-L47)

#### Catalog Service - Companies
**Pattern**: `/api/companies/**`
**Target**: `lb://CATALOG-SERVICE`
**Métodos**: GET, POST, PUT, DELETE
**Circuit Breaker**: `catalogServiceCircuitBreaker`
**Fallback**: `forward:/fallback/catalog`
**Clase responsable**: Spring Cloud Gateway
**Definición**: [application.yml:50-58](src/main/resources/application.yml#L50-L58)

**Nota**: Las rutas específicas (`/api/companies/buses/**`, `/api/companies/drivers/**`) están declaradas **antes** de la ruta genérica (`/api/companies/**`) para evitar conflictos de matching. Spring Cloud Gateway evalúa rutas en el orden declarado.

#### User Service
**Pattern**: `/api/users/**`
**Target**: `lb://USER-SERVICE`
**Métodos**: GET, POST, PUT, DELETE
**Circuit Breaker**: `userServiceCircuitBreaker`
**Fallback**: `forward:/fallback/users`
**Clase responsable**: Spring Cloud Gateway
**Definición**: [application.yml:61-69](src/main/resources/application.yml#L61-L69)

### 4.2 Endpoints propios del Gateway

#### Fallback Endpoints
- `GET /fallback/catalog` → [FallbackController.java:17-27](src/main/java/com/busconnect/apigateway/controller/FallbackController.java#L17-L27)
- `GET /fallback/users` → [FallbackController.java:29-39](src/main/java/com/busconnect/apigateway/controller/FallbackController.java#L29-L39)

#### Actuator Endpoints
- `GET /actuator/health` → Health check completo
- `GET /actuator/info` → Información de aplicación
- `GET /actuator/gateway/routes` → Rutas configuradas
- `GET /actuator/metrics` → Métricas de sistema

**Configuración**: [application.yml:116-125](src/main/resources/application.yml#L116-L125)

---

## 5. Flujo de una Request

### 5.1 Flujo completo paso a paso

#### Ejemplo: `POST /api/users` (creación de usuario)

**Paso 1: Recepción de la petición**
- Cliente HTTP envía: `POST http://api-gateway:8080/api/users` con body JSON
- Puerto de escucha: `8080` (definido en [application.yml:2](src/main/resources/application.yml#L2))
- Reactor Netty (servidor web subyacente de WebFlux) recibe la petición

**Paso 2: Filtro de Correlation ID**
- **Clase**: `GatewayConfig.requestTracingFilter()` ([GatewayConfig.java:17-37](src/main/java/com/busconnect/apigateway/config/GatewayConfig.java#L17-L37))
- **Orden de ejecución**: `-1` (primer filtro)
- **Lógica**:
  ```java
  String correlationId = exchange.getRequest().getHeaders()
          .getFirst("X-Correlation-ID");
  if (correlationId == null) {
      correlationId = UUID.randomUUID().toString();
  }
  ```
- **Resultado**: Header `X-Correlation-ID` presente en la petición hacia el backend

**Paso 3: Filtro de Logging (Pre-request)**
- **Clase**: `GatewayConfig.loggingFilter()` ([GatewayConfig.java:41-57](src/main/java/com/busconnect/apigateway/config/GatewayConfig.java#L41-L57))
- **Orden de ejecución**: `0` (segundo filtro)
- **Lógica**:
  ```java
  long startTime = System.currentTimeMillis();
  String path = exchange.getRequest().getPath().value();
  String method = exchange.getRequest().getMethod().name();
  ```
- **Acción**: Captura timestamp de inicio, método y path

**Paso 4: Evaluación CORS (si aplica)**
- **Configuración**: [application.yml:72-96](src/main/resources/application.yml#L72-L96)
- **Escenario**: Si la petición incluye header `Origin`
- **Validaciones**:
  - ¿El origen está en `allowedOrigins`?
  - ¿El método está en `allowedMethods`?
- **Resultado**: Si es OPTIONS (preflight), responde inmediatamente con headers CORS

**Paso 5: Matching de ruta**
- **Motor**: Spring Cloud Gateway RouteLocator
- **Predicado evaluado**: `Path=/api/users/**` ([application.yml:64](src/main/resources/application.yml#L64))
- **Resultado**: Match con ruta `user-service` (id: `user-service`)

**Paso 6: Filtro de deduplicación**
- **Configuración**: [application.yml:12](src/main/resources/application.yml#L12)
- **Filtro**: `DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin`
- **Lógica**: Elimina duplicados de headers CORS en la respuesta

**Paso 7: Resolución de servicio en Eureka**
- **URI configurada**: `lb://USER-SERVICE` ([application.yml:62](src/main/resources/application.yml#L62))
- **Eureka Client**: Consulta registro de `USER-SERVICE` en Eureka Server
- **Configuración Eureka**: [application.yml:99-113](src/main/resources/application.yml#L99-L113)
- **Resultado**: Obtiene lista de instancias disponibles (ej: `user-service:8082`)

**Paso 8: Load Balancing**
- **Mecanismo**: Ribbon (integrado en Eureka Client)
- **Estrategia**: Round Robin (por defecto)
- **Acción**: Selecciona una instancia de `USER-SERVICE`

**Paso 9: Circuit Breaker Check**
- **Filtro**: `CircuitBreaker` ([application.yml:66-69](src/main/resources/application.yml#L66-L69))
- **Instancia**: `userServiceCircuitBreaker`
- **Configuración**: [application.yml:140-141](src/main/resources/application.yml#L140-L141)
- **Estados posibles**:
  - **CLOSED**: Permite la petición (estado normal)
  - **OPEN**: Cortocircuita → envía a fallback sin llamar al servicio
  - **HALF_OPEN**: Permite peticiones de prueba

**Paso 10: Timeout Configuration**
- **Configuración**: [application.yml:149-150](src/main/resources/application.yml#L149-L150)
- **Timeout**: `10s` (TimeLimiter de Resilience4j)
- **Acción**: Lanza `TimeoutException` si el servicio no responde en 10 segundos

**Paso 11: Forward de la petición**
- **Destino**: `http://user-service:8082/api/users` (resuelta por Eureka)
- **Headers añadidos**:
  - `X-Correlation-ID: {uuid}`
  - Otros headers del cliente original
- **Body**: Transmitido sin modificación

**Paso 12: Respuesta del microservicio**
- **Ejemplo**: `201 Created` con body JSON del usuario creado
- **Headers**: Los que retorne el microservicio

**Paso 13: Circuit Breaker Post-processing**
- **Lógica**: Registra resultado de la petición (éxito/fallo)
- **Actualización de métricas**:
  - Sliding window: últimas 10 llamadas
  - Tasa de fallos: calculada en tiempo real
- **Transición de estado**: Si tasa de fallos > 50% → OPEN

**Paso 14: Filtro de Logging (Post-response)**
- **Clase**: `GatewayConfig.loggingFilter()` ([GatewayConfig.java:48-55](src/main/java/com/busconnect/apigateway/config/GatewayConfig.java#L48-L55))
- **Lógica**:
  ```java
  long duration = System.currentTimeMillis() - startTime;
  int statusCode = exchange.getResponse().getStatusCode().value();
  System.out.printf("[GATEWAY] %s %s -> %d (%dms)%n",
          method, path, statusCode, duration);
  ```
- **Output ejemplo**: `[GATEWAY] POST /api/users -> 201 (245ms)`

**Paso 15: Filtro de Correlation ID (Post-response)**
- **Clase**: `GatewayConfig.requestTracingFilter()` ([GatewayConfig.java:32-35](src/main/java/com/busconnect/apigateway/config/GatewayConfig.java#L32-L35))
- **Lógica**:
  ```java
  exchange.getResponse().getHeaders()
          .add("X-Correlation-ID", finalCorrelationId);
  ```
- **Resultado**: Header `X-Correlation-ID` presente en respuesta al cliente

**Paso 16: Respuesta al cliente**
- **Status**: `201 Created`
- **Headers**:
  - `Content-Type: application/json`
  - `X-Correlation-ID: {uuid}`
  - Headers CORS (si aplica)
- **Body**: Datos del usuario creado

### 5.2 Flujo alternativo: Circuit Breaker abierto

Si en el **Paso 9** el Circuit Breaker está en estado **OPEN**:

**Paso 9b: Fallback inmediato**
- **No se llama** al microservicio backend
- **Forward interno**: `forward:/fallback/users` ([application.yml:69](src/main/resources/application.yml#L69))

**Paso 9c: FallbackController procesa**
- **Método**: `FallbackController.usersFallback()` ([FallbackController.java:30-39](src/main/java/com/busconnect/apigateway/controller/FallbackController.java#L30-L39))
- **Retorno**: `Mono<ResponseEntity<Map<String, Object>>>`
- **Status**: `503 SERVICE_UNAVAILABLE`
- **Body**:
  ```json
  {
    "status": "error",
    "service": "user-service",
    "message": "User service is temporarily unavailable. Please try again later.",
    "timestamp": "2026-02-07T10:35:12"
  }
  ```

**Paso 10-16**: Continúan normalmente con la respuesta de fallback

---

## 6. Programación Reactiva

### 6.1 Uso de Mono

#### En FallbackController
**Ubicación**: [FallbackController.java:18-26](src/main/java/com/busconnect/apigateway/controller/FallbackController.java#L18-L26)

**Código**:
```java
public Mono<ResponseEntity<Map<String, Object>>> catalogFallback() {
    return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(...)));
}
```

**Propósito en este gateway**:
- `Mono.just()` crea un stream reactivo que emite un único valor (la respuesta de fallback)
- Permite integración con el stack reactivo de Spring Cloud Gateway
- No hay operaciones asíncronas reales aquí (respuesta inmediata), pero mantiene la firma reactiva requerida

#### En GatewayConfig (Filtros)
**Ubicación**: [GatewayConfig.java:31-35](src/main/java/com/busconnect/apigateway/config/GatewayConfig.java#L31-L35)

**Código**:
```java
return chain.filter(exchange.mutate().request(request).build())
        .then(Mono.fromRunnable(() -> {
            exchange.getResponse().getHeaders()
                    .add("X-Correlation-ID", finalCorrelationId);
        }));
```

**Propósito en este gateway**:
- `chain.filter()` retorna `Mono<Void>` (representa el procesamiento asíncrono de la petición)
- `.then(Mono.fromRunnable())` ejecuta lógica **después** de que la petición se haya procesado completamente
- Permite modificar la respuesta de forma no bloqueante
- Mantiene la naturaleza reactiva del pipeline de filtros

### 6.2 Dónde se usa Flux

**Respuesta**: No se usa `Flux` directamente en el código del gateway.

**Razón**:
- `Flux` es para streams de múltiples elementos (0..N)
- El gateway maneja requests individuales (1 request = 1 respuesta)
- Internamente, Spring Cloud Gateway puede usar `Flux` para streaming de bytes del body, pero no está expuesto en el código de usuario

### 6.3 Por qué programación reactiva aquí

#### Evidencia técnica:
1. **Dependencia**: `spring-cloud-starter-gateway` ([pom.xml:23](pom.xml#L23))
   - Basado en Spring WebFlux (reactivo)
   - No compatible con Spring MVC (bloqueante)

2. **Servidor**: Reactor Netty
   - Event loop basado en Netty
   - Maneja miles de conexiones concurrentes con pocos threads

3. **Filtros**: `GlobalFilter` retorna `Mono<Void>` ([GatewayConfig.java:17](src/main/java/com/busconnect/apigateway/config/GatewayConfig.java#L17))
   - API reactiva de Spring Cloud Gateway

#### Implicaciones en este gateway:
- **Alta concurrencia**: Puede manejar muchas peticiones simultáneas sin colapsar
- **Eficiencia de recursos**: No bloquea threads esperando respuestas de microservicios
- **Backpressure**: Puede aplicar presión hacia upstream si downstream es lento
- **Non-blocking I/O**: Las llamadas a Eureka y microservicios no bloquean threads

#### Beneficios observables:
- Con 100 threads puede manejar 10,000+ conexiones concurrentes
- Timeouts no consumen threads bloqueados
- Circuit Breaker se integra nativamente con streams reactivos

---

## 7. Configuración

### 7.1 application.yml

#### Servidor
**Líneas**: [application.yml:1-2](src/main/resources/application.yml#L1-L2)
```yaml
server:
  port: 8080
```
**Descripción**: Puerto de escucha del gateway.

#### Aplicación
**Líneas**: [application.yml:4-6](src/main/resources/application.yml#L4-L6)
```yaml
spring:
  application:
    name: api-gateway
```
**Descripción**: Nombre con el que se registra en Eureka.

#### Filtros globales
**Líneas**: [application.yml:11-12](src/main/resources/application.yml#L11-L12)
```yaml
default-filters:
  - DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin
```
**Descripción**: Elimina headers CORS duplicados en respuestas.

#### Rutas
**Sección completa**: [application.yml:15-69](src/main/resources/application.yml#L15-L69)

**Estructura de cada ruta**:
- `id`: Identificador único de la ruta
- `uri`: URI del servicio destino (formato `lb://{SERVICE-NAME}` para load balancing)
- `predicates`: Condiciones de matching (Path, Method, Header, etc.)
- `filters`: Filtros específicos de la ruta (Circuit Breaker, Rewrite, Retry, etc.)

**Ejemplo**:
```yaml
- id: user-service
  uri: lb://USER-SERVICE
  predicates:
    - Path=/api/users/**
  filters:
    - name: CircuitBreaker
      args:
        name: userServiceCircuitBreaker
        fallbackUri: forward:/fallback/users
```

#### CORS global
**Líneas**: [application.yml:72-96](src/main/resources/application.yml#L72-L96)
```yaml
globalcors:
  cors-configurations:
    '[/**]':
      allowedOrigins:
        - "http://localhost:3000"
        - "http://localhost:4200"
        - "http://localhost:5173"
        - "https://busconnect-frontend-ja4x.onrender.com"
      allowedMethods:
        - GET
        - POST
        - PUT
        - DELETE
        - PATCH
        - OPTIONS
        - HEAD
      allowedHeaders:
        - "*"
      exposedHeaders:
        - Authorization
        - Content-Type
        - X-Correlation-ID
        - Location
      allowCredentials: true
      maxAge: 3600
```

**Descripción**:
- Aplica a todos los paths (`[/**]`)
- Permite múltiples orígenes de desarrollo y producción
- Expone headers personalizados (`X-Correlation-ID`)
- Habilita credenciales (cookies, Authorization header)
- Cache de preflight: 1 hora

#### Eureka Client
**Líneas**: [application.yml:99-113](src/main/resources/application.yml#L99-L113)
```yaml
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://localhost:8761/eureka/}
    register-with-eureka: true
    fetch-registry: true
    registry-fetch-interval-seconds: 30
    initial-instance-info-replication-interval-seconds: 40
    instance-info-replication-interval-seconds: 30
    eureka-service-url-poll-interval-seconds: 60
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${random.value}
    lease-renewal-interval-in-seconds: 30
    lease-expiration-duration-in-seconds: 90
```

**Parámetros clave**:
- `register-with-eureka: true`: El gateway se registra como servicio
- `fetch-registry: true`: Obtiene lista de servicios registrados
- `registry-fetch-interval-seconds: 30`: Actualiza caché cada 30s
- `prefer-ip-address: true`: Usa IP en lugar de hostname
- `instance-id`: Identificador único con valor aleatorio

#### Actuator
**Líneas**: [application.yml:116-125](src/main/resources/application.yml#L116-L125)
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,gateway,metrics
  endpoint:
    health:
      show-details: always
    gateway:
      enabled: true
```

**Descripción**:
- Expone endpoints de health, info, gateway routes y métricas
- Health check muestra detalles completos (conexión Eureka, estado de rutas)
- Endpoint `/actuator/gateway/routes` lista rutas configuradas

#### Circuit Breaker (Resilience4j)
**Líneas**: [application.yml:128-150](src/main/resources/application.yml#L128-L150)
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10000
        permittedNumberOfCallsInHalfOpenState: 5
        automaticTransitionFromOpenToHalfOpenEnabled: true
    instances:
      catalogServiceCircuitBreaker:
        baseConfig: default
      userServiceCircuitBreaker:
        baseConfig: default
  timelimiter:
    configs:
      default:
        timeoutDuration: 10s
    instances:
      catalogServiceCircuitBreaker:
        baseConfig: default
      userServiceCircuitBreaker:
        baseConfig: default
```

**Parámetros explicados**:
- `slidingWindowSize: 10`: Ventana deslizante de 10 llamadas
- `failureRateThreshold: 50`: Abre el circuito si 50% de llamadas fallan
- `waitDurationInOpenState: 10000`: Permanece abierto 10 segundos
- `permittedNumberOfCallsInHalfOpenState: 5`: Permite 5 llamadas de prueba
- `automaticTransitionFromOpenToHalfOpenEnabled: true`: Transición automática después del wait
- `timeoutDuration: 10s`: Timeout por llamada

#### Logging
**Líneas**: [application.yml:153-156](src/main/resources/application.yml#L153-L156)
```yaml
logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    reactor.netty: INFO
```

**Descripción**:
- Gateway en modo DEBUG para ver detalles de routing
- Reactor Netty en INFO para no saturar logs

#### Perfil Docker
**Líneas**: [application.yml:159-171](src/main/resources/application.yml#L159-L171)
```yaml
spring:
  config:
    activate:
      on-profile: docker

eureka:
  client:
    service-url:
      defaultZone: http://eureka-service:8761/eureka/
  instance:
    hostname: api-gateway
```

**Descripción**:
- Activo cuando `SPRING_PROFILES_ACTIVE=docker`
- Cambia URL de Eureka a nombre de servicio Docker (`eureka-service`)
- Define hostname como `api-gateway` para networking de Docker

### 7.2 Variables de entorno

#### EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
**Uso**: [application.yml:102](src/main/resources/application.yml#L102)
```yaml
defaultZone: ${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://localhost:8761/eureka/}
```

**Propósito**: URL del servidor Eureka.

**Valores según entorno**:
- **Local**: `http://localhost:8761/eureka/`
- **Docker**: `http://eureka-service:8761/eureka/`
- **Producción**: URL del cluster de Eureka

#### SPRING_PROFILES_ACTIVE
**Uso**: Activa perfiles de configuración.

**Valores posibles**:
- `default`: Configuración local
- `docker`: Configuración para contenedores
- No especificada en application.yml, se pasa como variable de entorno

### 7.3 Configuración de Docker

#### Multi-stage build
**Ubicación**: [Dockerfile](Dockerfile)

**Stage 1 - dependencies** (líneas 1-14):
```dockerfile
FROM maven:3.9-eclipse-temurin-21-alpine AS dependencies
WORKDIR /app
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
RUN mvn dependency:go-offline -pl api-gateway -am -B
```
**Propósito**: Descarga dependencias (cacheado hasta que cambie pom.xml).

**Stage 2 - builder** (líneas 16-25):
```dockerfile
FROM dependencies AS builder
COPY api-gateway/src api-gateway/src
RUN mvn clean package -pl api-gateway -am -DskipTests -B && \
    java -Djarmode=layertools -jar /app/api-gateway/target/*.jar extract --destination /app/extracted
```
**Propósito**: Compila aplicación y extrae layers de Spring Boot para optimizar cambios.

**Stage 3 - runtime** (líneas 27-62):
```dockerfile
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
RUN addgroup -g 1001 -S spring && adduser -u 1001 -S spring -G spring
COPY --from=builder --chown=spring:spring /app/extracted/dependencies/ ./
COPY --from=builder --chown=spring:spring /app/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /app/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /app/extracted/application/ ./
USER spring
```
**Propósito**: Imagen mínima de ejecución con usuario no-root.

#### Optimizaciones JVM
**Líneas**: [Dockerfile:54-60](Dockerfile#L54-L60)
```dockerfile
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+OptimizeStringConcat \
    -XX:+UseStringDeduplication \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom"
```

**Descripción**:
- `UseContainerSupport`: Detecta límites de memoria del contenedor
- `MaxRAMPercentage=75.0`: Usa máximo 75% de RAM del contenedor para heap
- `InitialRAMPercentage=50.0`: Heap inicial al 50%
- `OptimizeStringConcat`: Optimiza concatenación de strings
- `UseStringDeduplication`: Deduplica strings en memoria
- `ExitOnOutOfMemoryError`: Termina proceso en OOM (permite restart de contenedor)
- `java.security.egd`: Usa /dev/urandom para entropía (más rápido en contenedores)

#### Health check
**Líneas**: [Dockerfile:48-49](Dockerfile#L48-L49)
```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
```

**Descripción**:
- Verifica cada 30 segundos
- Timeout de 10 segundos
- Espera 60 segundos después de iniciar (startup time)
- Marca unhealthy después de 3 fallos consecutivos

---

## 8. Integración con otros Microservicios

### 8.1 Servicios enrutados

#### CATALOG-SERVICE
**Descubrimiento**: Registrado en Eureka con nombre `CATALOG-SERVICE`

**Rutas que consume**:
- `/api/routes/**` → Gestión de rutas de autobús
- `/api/companies/**` → CRUD de empresas de transporte
- `/api/companies/buses/**` → CRUD de buses
- `/api/companies/drivers/**` → CRUD de conductores

**Configuración**: [application.yml:17-58](src/main/resources/application.yml#L17-L58)

**Protocolo de comunicación**: HTTP/REST sobre WebFlux reactivo

**Resolución de instancia**:
1. Gateway consulta Eureka: "¿Dónde está CATALOG-SERVICE?"
2. Eureka responde con lista de instancias (ej: `catalog-service:8083`)
3. Ribbon selecciona instancia (Round Robin)
4. Gateway hace forward HTTP

**Circuit Breaker**: `catalogServiceCircuitBreaker`

**Fallback**: [FallbackController.catalogFallback()](src/main/java/com/busconnect/apigateway/controller/FallbackController.java#L17-L27)

#### USER-SERVICE
**Descubrimiento**: Registrado en Eureka con nombre `USER-SERVICE`

**Rutas que consume**:
- `/api/users/**` → CRUD de usuarios

**Configuración**: [application.yml:61-69](src/main/resources/application.yml#L61-L69)

**Protocolo de comunicación**: HTTP/REST sobre WebFlux reactivo

**Resolución de instancia**: Mismo mecanismo que CATALOG-SERVICE

**Circuit Breaker**: `userServiceCircuitBreaker`

**Fallback**: [FallbackController.usersFallback()](src/main/java/com/busconnect/apigateway/controller/FallbackController.java#L30-L39)

### 8.2 EUREKA-SERVICE (Service Discovery)

**Rol**: Registry central de microservicios.

**URL configurada**:
- Local: `http://localhost:8761/eureka/`
- Docker: `http://eureka-service:8761/eureka/`

**Protocolo de comunicación**: HTTP REST (Eureka REST API)

**Operaciones del Gateway hacia Eureka**:
1. **Registro (Heartbeat)**:
   - Frecuencia: cada 30s ([application.yml:112](src/main/resources/application.yml#L112))
   - Endpoint: `POST /eureka/apps/API-GATEWAY`
   - Payload: Metadatos de instancia (IP, puerto, status)

2. **Fetch Registry**:
   - Frecuencia: cada 30s ([application.yml:105](src/main/resources/application.yml#L105))
   - Endpoint: `GET /eureka/apps`
   - Respuesta: Lista de todos los servicios registrados

3. **Renovación de Lease**:
   - Si no hay heartbeat en 90s, Eureka marca instancia como DOWN ([application.yml:113](src/main/resources/application.yml#L113))

**Cómo se realiza la comunicación**:
- Cliente HTTP: `RestTemplate` (incluido en Eureka Client)
- Caché local: El gateway mantiene caché de servicios (actualizado cada 30s)
- Resiliencia: Si Eureka está down, el gateway usa última versión del caché

### 8.3 Diagrama de comunicación

```
┌─────────────┐
│   Cliente   │
│  (Browser)  │
└──────┬──────┘
       │ HTTP/REST
       │ POST /api/users
       ▼
┌─────────────────────┐
│   API Gateway       │
│   (puerto 8080)     │
│                     │
│ 1. Filtros globales │
│ 2. Match ruta       │
│ 3. Consulta Eureka  │◄──────────┐
│ 4. Circuit Breaker  │           │
│ 5. Forward request  │           │ HTTP REST
└──────┬──────────────┘           │ Registry queries
       │                          │
       │                     ┌────┴────────┐
       │ Load Balanced       │   Eureka    │
       │ HTTP/REST           │   Server    │
       │                     │ (8761)      │
       ▼                     └─────────────┘
┌─────────────────────┐           ▲
│   USER-SERVICE      │           │ Heartbeat
│   (puerto 8082)     │───────────┤ (cada 30s)
│                     │           │
│ Lógica de negocio   │           │
│ Persistencia        │           │
└─────────────────────┘           │
                                  │
┌─────────────────────┐           │
│  CATALOG-SERVICE    │           │
│  (puerto 8083)      │───────────┘
│                     │
│ Lógica de negocio   │
│ Persistencia        │
└─────────────────────┘
```

---

## 9. Seguridad

### 9.1 Estado actual: NO implementada

**Evidencia**:
- No hay dependencias de `spring-cloud-starter-security` en [pom.xml](pom.xml)
- No hay clases de configuración de seguridad en `src/main/java/`
- No hay filtros de autenticación JWT/OAuth2
- No hay validación de tokens en los filtros globales

### 9.2 Implicaciones técnicas

**Riesgos actuales**:
- Cualquier cliente puede acceder a todos los endpoints sin autenticación
- No hay control de autorización (roles, permisos)
- No hay protección contra ataques de fuerza bruta
- No hay rate limiting por usuario/IP

**Mitigaciones actuales**:
- CORS configurado: limita orígenes permitidos ([application.yml:72-96](src/main/resources/application.yml#L72-L96))
- Circuit Breaker: previene cascadas de fallos
- Timeout: previene peticiones infinitas

### 9.3 CORS como única capa de seguridad

**Configuración**: [application.yml:72-96](src/main/resources/application.yml#L72-L96)

**Protección**:
- **Navegadores**: CORS previene que scripts maliciosos en otros dominios accedan a la API
- **No navegadores**: CORS no protege contra herramientas como `curl`, Postman, scripts Python

**Orígenes permitidos**:
```yaml
allowedOrigins:
  - "http://localhost:3000"          # React dev
  - "http://localhost:4200"          # Angular dev
  - "http://localhost:5173"          # Vite dev
  - "https://busconnect-frontend-ja4x.onrender.com"  # Producción
```

**Credenciales habilitadas**: `allowCredentials: true`
- Permite cookies, Authorization header
- Requiere origen específico (no puede ser `*`)

**Headers expuestos**:
```yaml
exposedHeaders:
  - Authorization
  - Content-Type
  - X-Correlation-ID
  - Location
```
**Propósito**: Permite que JavaScript del cliente lea estos headers de la respuesta.

### 9.4 Seguridad de infraestructura

#### Dockerfile
**Usuario no-root**: [Dockerfile:36-37](Dockerfile#L36-L37)
```dockerfile
RUN addgroup -g 1001 -S spring && adduser -u 1001 -S spring -G spring
USER spring
```
**Propósito**: Previene escalación de privilegios si el contenedor es comprometido.

#### Gestión de excepciones
**Fallback Controllers**: [FallbackController.java](src/main/java/com/busconnect/apigateway/controller/FallbackController.java)

**Respuestas de error controladas**:
- No exponen stack traces
- Mensajes genéricos: "Service temporarily unavailable"
- No filtran información sensible

---

## 10. Observabilidad y Manejo de Errores

### 10.1 Logging

#### Nivel de logs configurado
**Ubicación**: [application.yml:153-156](src/main/resources/application.yml#L153-L156)
```yaml
logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    reactor.netty: INFO
```

**Salida esperada**:
- **DEBUG de Gateway**: Muestra matching de rutas, filtros aplicados, resolución de URIs
- **INFO de Reactor Netty**: Errores de conexión, timeouts, cierre de canales

#### Filtro de logging personalizado
**Clase**: `GatewayConfig.loggingFilter()` ([GatewayConfig.java:41-57](src/main/java/com/busconnect/apigateway/config/GatewayConfig.java#L41-L57))

**Implementación**:
```java
@Bean
@Order(0)
public GlobalFilter loggingFilter() {
    return (exchange, chain) -> {
        long startTime = System.currentTimeMillis();
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    int statusCode = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value()
                            : 0;
                    System.out.printf("[GATEWAY] %s %s -> %d (%dms)%n",
                            method, path, statusCode, duration);
                }));
    };
}
```

**Ejemplo de salida**:
```
[GATEWAY] GET /api/users/123 -> 200 (45ms)
[GATEWAY] POST /api/companies -> 201 (123ms)
[GATEWAY] GET /api/routes -> 503 (10005ms)
```

**Información registrada**:
- Método HTTP
- Path completo
- Status code de respuesta
- Duración total en milisegundos

#### Correlation ID para trazabilidad
**Clase**: `GatewayConfig.requestTracingFilter()` ([GatewayConfig.java:17-37](src/main/java/com/busconnect/apigateway/config/GatewayConfig.java#L17-L37))

**Flujo**:
1. Si la petición incluye `X-Correlation-ID`, lo propaga
2. Si no, genera un UUID y lo añade
3. El header se envía al microservicio backend
4. El header se incluye en la respuesta al cliente

**Propósito**:
- Rastrear peticiones a través de múltiples microservicios
- Correlacionar logs de diferentes servicios
- Debugging de flujos distribuidos

**Ejemplo de uso**:
```bash
# Petición
curl -H "X-Correlation-ID: abc-123" http://localhost:8080/api/users/1

# Logs del Gateway
[GATEWAY] GET /api/users/1 -> 200 (45ms)
# Correlation-ID: abc-123

# Logs del User Service
[USER-SERVICE] GET /users/1 - Correlation-ID: abc-123

# Respuesta incluye el header
X-Correlation-ID: abc-123
```

### 10.2 Actuator Endpoints

**Configuración**: [application.yml:116-125](src/main/resources/application.yml#L116-L125)

#### /actuator/health
**Método**: GET

**Propósito**: Health check completo del gateway.

**Respuesta exitosa**:
```json
{
  "status": "UP",
  "components": {
    "discoveryComposite": {
      "status": "UP",
      "components": {
        "eureka": {
          "status": "UP",
          "details": {
            "applications": {
              "USER-SERVICE": 1,
              "CATALOG-SERVICE": 1
            }
          }
        }
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

**Status posibles**:
- `UP`: Todos los componentes funcionando
- `DOWN`: Componente crítico caído (ej: Eureka no accesible)
- `OUT_OF_SERVICE`: Servicio deshabilitado manualmente

#### /actuator/gateway/routes
**Método**: GET

**Propósito**: Lista todas las rutas configuradas en el gateway.

**Respuesta** (ejemplo parcial):
```json
[
  {
    "route_id": "user-service",
    "route_definition": {
      "id": "user-service",
      "predicates": [
        {
          "name": "Path",
          "args": {
            "pattern": "/api/users/**"
          }
        }
      ],
      "filters": [
        {
          "name": "CircuitBreaker",
          "args": {
            "name": "userServiceCircuitBreaker",
            "fallbackUri": "forward:/fallback/users"
          }
        }
      ],
      "uri": "lb://USER-SERVICE",
      "order": 0
    }
  }
]
```

**Uso**: Debugging de configuración de rutas.

#### /actuator/metrics
**Método**: GET

**Propósito**: Métricas del sistema.

**Métricas disponibles**:
- `resilience4j.circuitbreaker.state`: Estado de circuit breakers
- `resilience4j.circuitbreaker.calls`: Llamadas exitosas/fallidas
- `jvm.memory.used`: Memoria JVM
- `http.server.requests`: Estadísticas de peticiones HTTP

**Ejemplo de consulta específica**:
```bash
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state

{
  "name": "resilience4j.circuitbreaker.state",
  "measurements": [
    {
      "statistic": "VALUE",
      "value": 0.0  # 0=CLOSED, 1=OPEN, 2=HALF_OPEN
    }
  ],
  "availableTags": [
    {
      "tag": "name",
      "values": ["catalogServiceCircuitBreaker", "userServiceCircuitBreaker"]
    }
  ]
}
```

### 10.3 Manejo de excepciones

#### Timeouts
**Configuración**: [application.yml:142-150](src/main/resources/application.yml#L142-L150)

**Timeout por llamada**: `10s`

**Comportamiento**:
1. Si microservicio no responde en 10s → `TimeoutException`
2. Circuit Breaker registra como fallo
3. Si se supera umbral (50% de fallos) → Circuit Breaker abre
4. Próxima petición → Fallback inmediato (sin esperar timeout)

**Evidencia de timeout en logs**:
```
[GATEWAY] GET /api/users -> 503 (10005ms)
```
**Nota**: Duración ≈10s indica timeout.

#### Circuit Breaker - Estados y transiciones

**Configuración**: [application.yml:128-141](src/main/resources/application.yml#L128-L141)

**Estado CLOSED (normal)**:
- Todas las peticiones pasan al microservicio
- Se monitorean últimas 10 llamadas (sliding window)
- Se calcula tasa de fallos en tiempo real

**Transición CLOSED → OPEN**:
- **Condición**: Tasa de fallos ≥ 50% en ventana de 10 llamadas
- **Fallos contabilizados**: Timeouts, excepciones, status 5xx

**Estado OPEN (circuito abierto)**:
- **Duración**: 10 segundos
- **Comportamiento**: Todas las peticiones van a fallback sin llamar al servicio
- **Propósito**: Dar tiempo al servicio backend para recuperarse

**Transición OPEN → HALF_OPEN**:
- **Trigger**: Automático después de 10s
- **Configuración**: `automaticTransitionFromOpenToHalfOpenEnabled: true`

**Estado HALF_OPEN (probando recuperación)**:
- **Llamadas permitidas**: 5 (configuración: `permittedNumberOfCallsInHalfOpenState`)
- **Si todas 5 tienen éxito** → Transición a CLOSED
- **Si cualquiera falla** → Vuelve a OPEN

#### Fallback Controllers

**Catalog Service Fallback**:
```java
@GetMapping("/fallback/catalog")
public Mono<ResponseEntity<Map<String, Object>>> catalogFallback() {
    return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                    "status", "error",
                    "service", "catalog-service",
                    "message", "Catalog service is temporarily unavailable. Please try again later.",
                    "timestamp", LocalDateTime.now().toString()
            )));
}
```

**User Service Fallback**:
```java
@GetMapping("/fallback/users")
public Mono<ResponseEntity<Map<String, Object>>> usersFallback() {
    return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                    "status", "error",
                    "service", "user-service",
                    "message", "User service is temporarily unavailable. Please try again later.",
                    "timestamp", LocalDateTime.now().toString()
            )));
}
```

**Características**:
- Status HTTP: `503 SERVICE_UNAVAILABLE`
- Respuesta JSON estructurada
- Identifica servicio afectado
- Timestamp del error
- Mensaje user-friendly (no técnico)

### 10.4 Retries

**Estado actual**: NO implementados.

**Evidencia**:
- No hay filtro `Retry` en las rutas configuradas
- No hay configuración de `resilience4j.retry` en application.yml

**Implicación**: Si una petición falla, no se reintenta automáticamente.

---

## 11. Decisiones Técnicas Detectadas

### 11.1 Arquitectura reactiva (Spring WebFlux)

**Decisión**: Usar Spring Cloud Gateway (reactivo) en lugar de Netflix Zuul (bloqueante).

**Evidencia**:
- Dependencia: `spring-cloud-starter-gateway` ([pom.xml:23](pom.xml#L23))
- Métodos retornan `Mono<>` ([FallbackController.java:18](src/main/java/com/busconnect/apigateway/controller/FallbackController.java#L18))
- Filtros usan API reactiva: `GlobalFilter` con `Mono<Void>` ([GatewayConfig.java:17](src/main/java/com/busconnect/apigateway/config/GatewayConfig.java#L17))

**Implicaciones técnicas**:
- **Ventaja**: Mayor throughput con menos threads (eficiente para I/O bound)
- **Ventaja**: Backpressure nativo para controlar flujo de datos
- **Desventaja**: Curva de aprendizaje más alta (programación reactiva)
- **Desventaja**: Debugging más complejo (stack traces asíncronos)
- **Restricción**: No compatible con dependencias bloqueantes (JDBC tradicional, etc.)

### 11.2 Service Discovery con Eureka

**Decisión**: Usar Netflix Eureka para descubrimiento de servicios.

**Evidencia**:
- Dependencia: `spring-cloud-starter-netflix-eureka-client` ([pom.xml:29](pom.xml#L29))
- Anotación: `@EnableDiscoveryClient` ([ApiGatewayApplication.java:8](src/main/java/com/busconnect/apigateway/ApiGatewayApplication.java#L8))
- URIs con prefijo `lb://`: `lb://USER-SERVICE` ([application.yml:62](src/main/resources/application.yml#L62))

**Implicaciones técnicas**:
- **Ventaja**: Descubrimiento dinámico de instancias (no hardcodear IPs)
- **Ventaja**: Load balancing automático con Ribbon
- **Ventaja**: Alta disponibilidad (múltiples instancias por servicio)
- **Desventaja**: Dependencia de Eureka Server (punto único de fallo si no está clustereado)
- **Desventaja**: Latencia adicional en startup (registro en Eureka toma ~40s)

### 11.3 Circuit Breaker con Resilience4j

**Decisión**: Usar Resilience4j en lugar de Netflix Hystrix (deprecated).

**Evidencia**:
- Dependencia: `spring-cloud-starter-circuitbreaker-reactor-resilience4j` ([pom.xml:48](pom.xml#L48))
- Configuración: `resilience4j.circuitbreaker` ([application.yml:128](src/main/resources/application.yml#L128))
- Filtro en rutas: `CircuitBreaker` ([application.yml:22](src/main/resources/application.yml#L22))

**Implicaciones técnicas**:
- **Ventaja**: Hystrix está en modo mantenimiento, Resilience4j es activamente desarrollado
- **Ventaja**: Más ligero (no requiere HystrixCommand)
- **Ventaja**: Métricas expuestas vía Actuator/Micrometer
- **Configuración**: Requiere ajuste de parámetros por entorno (dev vs prod)

### 11.4 Rutas específicas antes de genéricas

**Decisión**: Declarar rutas `/api/companies/buses/**` y `/api/companies/drivers/**` antes de `/api/companies/**`.

**Evidencia**: [application.yml:28-58](src/main/resources/application.yml#L28-L58)
```yaml
# Rutas específicas primero
- id: catalog-service-buses
  predicates:
    - Path=/api/companies/buses/**

- id: catalog-service-drivers
  predicates:
    - Path=/api/companies/drivers/**

# Ruta general al final
- id: catalog-service-companies
  predicates:
    - Path=/api/companies/**
```

**Implicaciones técnicas**:
- **Razón**: Spring Cloud Gateway evalúa rutas en orden declarado
- **Problema evitado**: Si `/api/companies/**` estuviera primero, capturaría también `/api/companies/buses/1`
- **Patrón**: Más específico → menos específico

### 11.5 Filtros globales vs filtros por ruta

**Decisión**: Usar filtros globales para cross-cutting concerns (Correlation ID, logging) y filtros por ruta para circuit breakers.

**Evidencia**:
- **Globales**: `GatewayConfig` define beans de `GlobalFilter` ([GatewayConfig.java:17](src/main/java/com/busconnect/apigateway/config/GatewayConfig.java#L17))
- **Por ruta**: Circuit Breaker definido en cada ruta ([application.yml:22](src/main/resources/application.yml#L22))

**Implicaciones técnicas**:
- **Ventaja**: Separation of concerns (concerns transversales centralizados)
- **Ventaja**: Circuit Breaker por servicio permite configuración granular
- **Mantenibilidad**: Fácil añadir nuevo servicio (copiar/modificar ruta existente)

### 11.6 CORS global en lugar de por servicio

**Decisión**: Configurar CORS una vez en el gateway en lugar de en cada microservicio.

**Evidencia**: [application.yml:72-96](src/main/resources/application.yml#L72-L96)

**Implicaciones técnicas**:
- **Ventaja**: Configuración centralizada (DRY)
- **Ventaja**: Los microservicios backend no necesitan configurar CORS
- **Desventaja**: Si se necesitan políticas CORS diferentes por servicio, requiere lógica adicional
- **Riesgo**: El filtro `DedupeResponseHeader` es necesario para evitar duplicados si backend también configura CORS

### 11.7 Multi-stage Dockerfile

**Decisión**: Usar build de 3 stages (dependencies, builder, runtime).

**Evidencia**: [Dockerfile:1-62](Dockerfile#L1-L62)

**Implicaciones técnicas**:
- **Ventaja**: Cacheo agresivo de dependencias Maven (stage 1 se cachea hasta que cambie pom.xml)
- **Ventaja**: Imagen final mínima (JRE en lugar de JDK + Maven)
- **Ventaja**: Spring Boot Layertools optimiza cambios (dependencies cambian menos que application code)
- **Tamaño**: Imagen final ~200MB vs ~600MB sin multi-stage
- **Tiempo de build**: Rebuild incremental más rápido (solo recompila application layer)

### 11.8 Usuario no-root en contenedor

**Decisión**: Ejecutar aplicación con usuario `spring:spring` (UID/GID 1001).

**Evidencia**: [Dockerfile:36-45](Dockerfile#L36-L45)

**Implicaciones técnicas**:
- **Seguridad**: Previene escalación de privilegios si contenedor es comprometido
- **Best practice**: Cumple con estándares de seguridad de contenedores
- **Restricción**: El usuario no puede escribir en paths que requieren root (ej: /var/log)

### 11.9 JVM optimizations para contenedores

**Decisión**: Configurar flags JVM específicos para ejecución en contenedores.

**Evidencia**: [Dockerfile:54-60](Dockerfile#L54-L60)

**Implicaciones técnicas**:
- `UseContainerSupport`: JVM detecta límites de cgroup (CPU/memoria del contenedor)
- `MaxRAMPercentage=75.0`: Sin esta flag, JVM podría usar límite de host (ej: 16GB) en lugar de contenedor (ej: 512MB)
- `ExitOnOutOfMemoryError`: Permite que orquestador (Kubernetes, Docker Swarm) reinicie contenedor
- **Resultado**: Menor probabilidad de OOMKilled en entornos de contenedores

### 11.10 Actuator expuesto sin seguridad

**Decisión**: Exponer endpoints de Actuator sin autenticación.

**Evidencia**:
- No hay configuración de seguridad para `/actuator/**`
- `show-details: always` expone detalles completos ([application.yml:123](src/main/resources/application.yml#L123))

**Implicaciones técnicas**:
- **Ventaja desarrollo**: Fácil debugging y monitoreo
- **Riesgo producción**: Endpoints como `/actuator/env` pueden exponer secretos (variables de entorno)
- **Recomendación**: En producción debería protegerse con autenticación o no exponerse públicamente

---

## 12. Limitaciones Actuales

### 12.1 Limitaciones técnicas

#### Sin autenticación/autorización
**Evidencia**: No hay dependencias de Spring Security en [pom.xml](pom.xml).

**Impacto**:
- Cualquier cliente puede acceder a todos los endpoints
- No hay validación de tokens JWT/OAuth2
- No hay control de roles/permisos

**Riesgo**: Exposición de datos sensibles, operaciones no autorizadas.

#### Sin rate limiting
**Evidencia**: No hay filtros de rate limiting en rutas ni configuración de Redis/Bucket4j.

**Impacto**:
- Un cliente puede hacer peticiones ilimitadas
- Vulnerable a ataques de denegación de servicio (DoS)
- No hay protección contra scraping masivo

#### Sin retries configurados
**Evidencia**: No hay filtro `Retry` en [application.yml](src/main/resources/application.yml).

**Impacto**:
- Fallos transitorios de red no se recuperan automáticamente
- Menor resiliencia ante errores temporales

**Razón posible**: Evitar complejidad (retries con circuit breaker pueden conflictuar).

#### Sin caché de respuestas
**Evidencia**: No hay filtro de caché ni integración con Redis/Caffeine.

**Impacto**:
- Todas las peticiones se forwardean al backend
- Mayor latencia y carga en microservicios

#### Sin versionado de API
**Evidencia**: Paths no incluyen versión (ej: `/api/v1/users`).

**Impacto**:
- Cambios breaking en backend afectan a todos los clientes
- No se pueden mantener múltiples versiones de API simultáneamente

#### Sin request/response transformation
**Evidencia**: No hay filtros `RewritePath` para modificar payloads.

**Impacto**:
- El gateway no puede adaptar diferentes formatos de API
- No puede ocultar estructura interna de backend

#### Actuator sin autenticación
**Evidencia**: Endpoints `/actuator/**` expuestos sin seguridad.

**Riesgo**: Exposición de configuración, métricas, variables de entorno.

### 12.2 Limitaciones funcionales

#### Solo enruta dos servicios
**Evidencia**: Solo están configurados `CATALOG-SERVICE` y `USER-SERVICE` ([application.yml:15-69](src/main/resources/application.yml#L15-L69)).

**Impacto**: Microservicios adicionales requieren añadir rutas manualmente.

#### Fallbacks genéricos
**Evidencia**: Fallbacks solo retornan error 503 con mensaje genérico ([FallbackController.java:24](src/main/java/com/busconnect/apigateway/controller/FallbackController.java#L24)).

**Limitación**: No hay respuestas de fallback con datos alternativos (ej: caché, default values).

#### Sin routing dinámico
**Evidencia**: Rutas definidas estáticamente en application.yml.

**Limitación**: Añadir/modificar rutas requiere redeploy del gateway.

#### Sin soporte para WebSockets
**Evidencia**: No hay configuración de WebSocket en [application.yml](src/main/resources/application.yml).

**Impacto**: No se pueden exponer conexiones WebSocket de microservicios backend.

### 12.3 Limitaciones de diseño

#### Circuit Breaker compartido por múltiples rutas
**Evidencia**: Todas las rutas de `CATALOG-SERVICE` usan el mismo circuit breaker `catalogServiceCircuitBreaker` ([application.yml:24](src/main/resources/application.yml#L24)).

**Implicación**:
- Si `/api/companies/buses` falla, también se abre el circuito para `/api/routes`
- No hay granularidad por endpoint

**Posible mejora**: Circuit breaker por ruta en lugar de por servicio.

#### Eureka como SPOF (Single Point of Failure)
**Evidencia**: Gateway depende de Eureka para descubrimiento ([application.yml:102](src/main/resources/application.yml#L102)).

**Impacto**:
- Si Eureka está down, el gateway no puede descubrir nuevas instancias
- Mitigation actual: Caché local de Eureka (actualizado cada 30s)

**Riesgo**: Si Eureka está down > tiempo de lease (90s), instancias no se renuevan.

#### Configuración de timeouts global
**Evidencia**: Timeout de 10s aplicado a todos los servicios ([application.yml:145](src/main/resources/application.yml#L145)).

**Limitación**: Operaciones lentas legítimas (ej: reports, uploads) pueden timeoutear.

**Posible mejora**: Timeouts configurables por ruta.

#### Logging a stdout en lugar de agregador central
**Evidencia**: `System.out.printf` en `loggingFilter` ([GatewayConfig.java:53](src/main/java/com/busconnect/apigateway/config/GatewayConfig.java#L53)).

**Limitación**:
- Logs no estructurados (no JSON)
- Sin integración con ELK, Splunk, etc.
- Difícil correlacionar logs entre servicios

#### Sin métricas personalizadas
**Evidencia**: Solo métricas por defecto de Actuator/Micrometer.

**Limitación**: No se registran métricas de negocio (ej: requests por servicio, latencia por ruta).

---

## 13. Posibles Extensiones Futuras

### 13.1 Seguridad

#### Autenticación JWT
**Indicador en diseño actual**:
- Header `Authorization` está en `exposedHeaders` de CORS ([application.yml:91](src/main/resources/application.yml#L91))
- Estructura de filtros globales permite añadir filtro de autenticación

**Implementación sugerida**:
- Filtro global que valide JWT antes de enrutar
- Extracción de claims (user ID, roles) y propagación a backend

#### Autorización basada en roles
**Indicador en diseño actual**:
- Estructura de filtros por ruta permite añadir filtros de autorización específicos

**Implementación sugerida**:
- Filtro por ruta que valide roles requeridos
- Configuración: `requiredRole: ADMIN` en cada ruta

### 13.2 Resiliencia

#### Rate limiting
**Indicador en diseño actual**:
- Spring Cloud Gateway soporta filtro `RequestRateLimiter`
- Estructura de filtros por ruta permite añadirlo fácilmente

**Implementación sugerida**:
```yaml
filters:
  - name: RequestRateLimiter
    args:
      redis-rate-limiter.replenishRate: 10  # tokens/segundo
      redis-rate-limiter.burstCapacity: 20
```

#### Retries con backoff
**Indicador en diseño actual**:
- Resilience4j soporta módulo `resilience4j-retry`
- Circuit Breaker ya configurado, retries se integrarían fácilmente

**Implementación sugerida**:
```yaml
resilience4j:
  retry:
    instances:
      catalogServiceRetry:
        maxAttempts: 3
        waitDuration: 1000
        retryExceptions:
          - java.net.ConnectException
```

### 13.3 Observabilidad

#### Integración con ELK/Splunk
**Indicador en diseño actual**:
- Logback (incluido por defecto en Spring Boot) soporta appenders de Logstash
- Header `X-Correlation-ID` ya implementado

**Implementación sugerida**:
- Añadir `logstash-logback-encoder` como dependencia
- Configurar appender de Logstash en `logback-spring.xml`

#### Métricas personalizadas con Micrometer
**Indicador en diseño actual**:
- Actuator expone métricas vía Micrometer
- Filtros globales son punto ideal para registrar métricas

**Implementación sugerida**:
```java
@Bean
public GlobalFilter metricsFilter(MeterRegistry registry) {
    return (exchange, chain) -> {
        Timer.Sample sample = Timer.start(registry);
        return chain.filter(exchange)
                .doFinally(signalType -> {
                    sample.stop(registry.timer("gateway.requests",
                            "method", exchange.getRequest().getMethod().name(),
                            "path", exchange.getRequest().getPath().value()));
                });
    };
}
```

#### Tracing distribuido con Spring Cloud Sleuth
**Indicador en diseño actual**:
- Header `X-Correlation-ID` ya propagado
- Sleuth se integraría fácilmente con WebFlux

**Implementación sugerida**:
- Añadir `spring-cloud-starter-sleuth` + `spring-cloud-sleuth-zipkin`
- Sleuth automáticamente añadiría `trace-id`, `span-id` a logs

### 13.4 Caché y optimización

#### Response caching
**Indicador en diseño actual**:
- Gateway procesa todas las respuestas (filtro global)
- Fácil interceptar y cachear

**Implementación sugerida**:
- Filtro `LocalResponseCache` de Spring Cloud Gateway
- Integración con Redis para caché distribuido

#### Request deduplication
**Indicador en diseño actual**:
- Ya hay filtro `DedupeResponseHeader` para headers

**Implementación sugerida**:
- Filtro personalizado que detecte requests duplicadas (mismo payload + path en ventana de tiempo)

### 13.5 Routing avanzado

#### Routing dinámico
**Indicador en diseño actual**:
- Gateway soporta `RefreshScope` para recargar rutas sin restart

**Implementación sugerida**:
- Rutas almacenadas en base de datos o config server
- Endpoint `/actuator/gateway/refresh` para recargar

#### WebSocket support
**Indicador en diseño actual**:
- Spring Cloud Gateway soporta WebSocket desde versión 2.1

**Implementación sugerida**:
```yaml
routes:
  - id: websocket-route
    uri: lb:ws://NOTIFICATION-SERVICE
    predicates:
      - Path=/ws/**
```

#### A/B Testing / Canary Deployments
**Indicador en diseño actual**:
- Eureka permite múltiples versiones del mismo servicio
- Gateway puede enrutar basándose en metadata

**Implementación sugerida**:
- Filtro personalizado que lea header `X-Version` o cookie
- Eureka metadata: `version: v2`
- Gateway enruta a instancias según versión

---

## 14. Apéndice

### 14.1 Mapa de archivos relevantes

#### Código fuente

| Archivo | Líneas | Propósito | Complejidad |
|---------|--------|-----------|-------------|
| [ApiGatewayApplication.java](src/main/java/com/busconnect/apigateway/ApiGatewayApplication.java) | 14 | Bootstrap de Spring Boot | Trivial |
| [GatewayConfig.java](src/main/java/com/busconnect/apigateway/config/GatewayConfig.java) | 59 | Filtros globales (Correlation ID, logging) | Baja |
| [FallbackController.java](src/main/java/com/busconnect/apigateway/controller/FallbackController.java) | 41 | Endpoints de fallback para Circuit Breaker | Trivial |

#### Configuración

| Archivo | Líneas | Propósito | Complejidad |
|---------|--------|-----------|-------------|
| [application.yml](src/main/resources/application.yml) | 171 | Configuración principal (rutas, CORS, Eureka, Circuit Breaker) | Alta |
| [pom.xml](pom.xml) | 62 | Dependencias Maven | Media |
| [Dockerfile](Dockerfile) | 63 | Build multi-stage de imagen Docker | Media |

#### Documentación

| Archivo | Propósito |
|---------|-----------|
| [README.md](README.md) | Documentación funcional y guía de uso |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Diagramas de flujo y arquitectura |
| **TECHNICAL_DOCUMENTATION.md** | Este documento (análisis técnico completo) |

### 14.2 Dependencias principales

#### Spring Cloud Gateway
**GroupId**: `org.springframework.cloud`
**ArtifactId**: `spring-cloud-starter-gateway`
**Propósito**: API Gateway reactivo basado en Spring WebFlux.
**Incluye**: Reactor Netty, WebFlux, Gateway Core.

#### Eureka Client
**GroupId**: `org.springframework.cloud`
**ArtifactId**: `spring-cloud-starter-netflix-eureka-client`
**Propósito**: Service Discovery y registro en Eureka Server.
**Incluye**: Eureka Client, Ribbon (load balancer).

#### Resilience4j
**GroupId**: `org.springframework.cloud`
**ArtifactId**: `spring-cloud-starter-circuitbreaker-reactor-resilience4j`
**Propósito**: Circuit Breaker pattern con integración reactiva.
**Incluye**: Resilience4j Core, Reactor adapter, Micrometer metrics.

#### Actuator
**GroupId**: `org.springframework.boot`
**ArtifactId**: `spring-boot-starter-actuator`
**Propósito**: Endpoints de monitoreo y métricas.
**Incluye**: Micrometer, Health checks, Info endpoint.

#### Lombok
**GroupId**: `org.projectlombok`
**ArtifactId**: `lombok`
**Propósito**: Reducción de boilerplate (aunque no se usa en el código actual).

### 14.3 Puertos y networking

| Componente | Puerto | Protocolo | Descripción |
|------------|--------|-----------|-------------|
| API Gateway | 8080 | HTTP | Punto de entrada de clientes |
| Eureka Server | 8761 | HTTP | Service Discovery |
| User Service | 8082 | HTTP | Backend (no expuesto externamente) |
| Catalog Service | 8083 | HTTP | Backend (no expuesto externamente) |

**Networking en Docker**:
- Gateway se comunica con Eureka vía hostname `eureka-service`
- Gateway se comunica con backends vía IPs resueltas por Eureka

### 14.4 Variables de entorno críticas

| Variable | Valor por defecto | Valor Docker | Propósito |
|----------|-------------------|--------------|-----------|
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://localhost:8761/eureka/` | `http://eureka-service:8761/eureka/` | URL de Eureka |
| `SPRING_PROFILES_ACTIVE` | (ninguno) | `docker` | Activa perfil de configuración |

### 14.5 Comandos útiles

#### Build y ejecución local
```bash
# Compilar
mvn clean package -DskipTests

# Ejecutar
java -jar target/api-gateway-0.0.1-SNAPSHOT.jar

# Con perfil Docker
java -jar target/api-gateway-0.0.1-SNAPSHOT.jar --spring.profiles.active=docker
```

#### Docker
```bash
# Build de imagen
docker build -t busconnect/api-gateway:latest .

# Ejecutar contenedor
docker run -p 8080:8080 \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-service:8761/eureka/ \
  busconnect/api-gateway:latest

# Ver logs
docker logs -f api-gateway
```

#### Endpoints de prueba
```bash
# Health check
curl http://localhost:8080/actuator/health

# Ver rutas configuradas
curl http://localhost:8080/actuator/gateway/routes | jq

# Probar enrutamiento a User Service
curl http://localhost:8080/api/users

# Probar enrutamiento a Catalog Service
curl http://localhost:8080/api/routes

# Ver métricas de Circuit Breaker
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state
```

### 14.6 Patrones de diseño identificados

#### API Gateway Pattern
**Descripción**: Punto de entrada único para múltiples microservicios.
**Implementación**: Toda la aplicación.

#### Circuit Breaker Pattern
**Descripción**: Previene cascadas de fallos cuando servicios están caídos.
**Implementación**: Resilience4j en cada ruta ([application.yml:22](src/main/resources/application.yml#L22)).

#### Service Registry Pattern
**Descripción**: Descubrimiento dinámico de ubicación de servicios.
**Implementación**: Eureka Client.

#### Bulkhead Pattern
**Descripción**: Aislamiento de recursos por servicio.
**Implementación**: Circuit Breaker separado por servicio.

#### Correlation ID Pattern
**Descripción**: Tracking de peticiones distribuidas.
**Implementación**: `requestTracingFilter` ([GatewayConfig.java:17](src/main/java/com/busconnect/apigateway/config/GatewayConfig.java#L17)).

### 14.7 Glosario técnico

**Circuit Breaker**: Mecanismo de protección que detiene peticiones a un servicio cuando tasa de fallos supera umbral.

**Eureka**: Service Discovery de Netflix para registro y descubrimiento de microservicios.

**Fallback**: Respuesta alternativa cuando un servicio no está disponible.

**Flux**: Stream reactivo de 0..N elementos (de Project Reactor).

**Load Balancing**: Distribución de peticiones entre múltiples instancias de un servicio.

**Mono**: Stream reactivo de 0..1 elemento (de Project Reactor).

**Predicate**: Condición que determina si una ruta se aplica a una petición.

**Reactor Netty**: Servidor web no bloqueante basado en Netty.

**Ribbon**: Load balancer client-side de Netflix (incluido en Eureka Client).

**Service Discovery**: Mecanismo para localizar dinámicamente instancias de servicios.

**Sliding Window**: Ventana deslizante de N llamadas para calcular métricas.

**Spring WebFlux**: Framework reactivo de Spring (alternativa a Spring MVC).

**Timeout**: Tiempo máximo de espera antes de considerar una operación fallida.

---

## Fin del Documento

**Fecha de generación**: 2026-02-07
**Versión analizada**: api-gateway 0.0.1-SNAPSHOT
**Commit base**: Verificar con `git log` en el repositorio
**Autor del análisis**: Claude Sonnet 4.5 (Modelo: claude-sonnet-4-5-20250929)

**Próximos pasos sugeridos**:
1. Implementar autenticación JWT (alta prioridad)
2. Añadir rate limiting
3. Configurar logging estructurado
4. Proteger endpoints de Actuator
5. Implementar métricas personalizadas
