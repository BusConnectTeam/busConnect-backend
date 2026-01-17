# API Gateway - BusConnect

## 📋 Descripción

API Gateway es el punto de entrada único para todos los microservicios del sistema BusConnect. Implementado con Spring Cloud Gateway, proporciona enrutamiento, balanceo de carga, circuit breakers y gestión de CORS para toda la arquitectura de microservicios.

## 🏗️ Arquitectura

Este gateway utiliza una arquitectura reactiva basada en **Spring WebFlux** y actúa como:

- **Punto de entrada único**: Todas las peticiones externas pasan por el gateway
- **Enrutador inteligente**: Distribuye las peticiones a los microservicios correspondientes
- **Balanceador de carga**: Utiliza Eureka para descubrimiento de servicios y load balancing
- **Protección con Circuit Breaker**: Implementa Resilience4j para tolerancia a fallos
- **Gestión de CORS**: Configuración centralizada de políticas CORS

## 🔧 Tecnologías

- **Spring Boot**: 3.3.13
- **Spring Cloud Gateway**: Enrutamiento reactivo
- **Spring Cloud Netflix Eureka Client**: Service Discovery
- **Resilience4j**: Circuit Breaker pattern
- **Spring Boot Actuator**: Health checks y métricas
- **Lombok**: Reducción de código boilerplate

## 📦 Configuración

### Variables de Entorno

| Variable | Descripción | Valor por Defecto |
|----------|-------------|-------------------|
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | URL del servidor Eureka | `http://localhost:8761/eureka/` |

### Puertos

- **Puerto del servicio**: `8080`
- **Eureka Server**: `8761`

## 🚀 Rutas Configuradas

### Catalog Service

```
GET/POST/PUT/DELETE /api/catalog/**
```

- **Target**: `CATALOG-SERVICE` (descubierto vía Eureka)
- **Reescritura**: `/api/catalog/routes` → `/routes`
- **Circuit Breaker**: `catalogServiceCircuitBreaker`
- **Fallback**: `/fallback/catalog`

### User Service

```
GET/POST/PUT/DELETE /api/users/**
```

- **Target**: `USER-SERVICE` (descubierto vía Eureka)
- **Reescritura**: `/api/users/profile` → `/profile`
- **Circuit Breaker**: `userServiceCircuitBreaker`
- **Fallback**: `/fallback/users`

## 🛡️ Circuit Breaker

Configuración de Resilience4j para cada servicio:

```yaml
- Sliding Window: 10 llamadas
- Failure Rate Threshold: 50%
- Wait Duration (Open State): 10s
- Timeout: 10s
```

### Estados del Circuit Breaker

1. **CLOSED**: Funcionamiento normal
2. **OPEN**: Circuito abierto tras superar el umbral de fallos
3. **HALF_OPEN**: Permite llamadas de prueba para verificar recuperación

## 🌐 Configuración CORS

CORS configurado globalmente para permitir:

- **Orígenes permitidos**: 
  - `http://localhost:3000` (React)
  - `http://localhost:4200` (Angular)
  - `http://localhost:5173` (Vite)
- **Métodos**: GET, POST, PUT, DELETE, PATCH, OPTIONS
- **Headers**: Todos (*)
- **Credentials**: Habilitadas
- **Max Age**: 3600s (1 hora)

## 📊 Endpoints de Actuator

- **Health**: `/actuator/health`
- **Info**: `/actuator/info`
- **Gateway Routes**: `/actuator/gateway/routes`
- **Metrics**: `/actuator/metrics`

### Ejemplo: Ver rutas configuradas

```bash
curl http://localhost:8080/actuator/gateway/routes
```

## 🐳 Docker

### Construcción de la imagen

```bash
docker build -t busconnect/api-gateway:latest .
```

### Variables de entorno en Docker

```yaml
environment:
  - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-service:8761/eureka/
```

## 🚀 Ejecución

### Desarrollo Local

```bash
# Opción 1: Maven
mvn spring-boot:run

# Opción 2: Java
mvn clean package
java -jar target/api-gateway-0.0.1-SNAPSHOT.jar
```

### Con Docker Compose

```bash
docker-compose up api-gateway
```

## 🔍 Monitoreo y Logs

### Ver logs en tiempo real

```bash
docker logs -f api-gateway
```

### Nivel de logging

- **Spring Cloud Gateway**: DEBUG
- **Reactor Netty**: INFO

## 📝 Estructura del Proyecto

```
api-gateway/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/busconnect/apigateway/
│       │       ├── ApiGatewayApplication.java
│       │       ├── config/
│       │       │   └── GatewayConfig.java
│       │       └── controller/
│       │           └── FallbackController.java
│       └── resources/
│           └── application.yml
├── Dockerfile
├── pom.xml
└── README.md
```

## 🧪 Pruebas

### Verificar conectividad

```bash
# Health check
curl http://localhost:8080/actuator/health

# Probar ruta de catalog
curl http://localhost:8080/api/catalog/routes

# Probar ruta de users
curl http://localhost:8080/api/users/1
```

### Verificar Circuit Breaker

```bash
# Ver métricas del circuit breaker
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state
```

## 🔐 Seguridad

### Buenas Prácticas Implementadas

- ✅ Sin exposición directa de microservicios
- ✅ Circuit breakers para prevenir cascadas de fallos
- ✅ Timeouts configurados (10s)
- ✅ CORS configurado apropiadamente
- ✅ Health checks habilitados

### Próximas Mejoras

- [ ] Autenticación y autorización con JWT
- [ ] Rate limiting
- [ ] Request/Response logging
- [ ] API versioning
- [ ] Request validation

## 📚 Dependencias Principales

```xml
<dependencies>
    <!-- Spring Cloud Gateway -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>
    
    <!-- Eureka Client -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    
    <!-- Circuit Breaker -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
    </dependency>
    
    <!-- Actuator -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>
```

## 🤝 Integración con Microservicios

### Registro en Eureka

El gateway se registra automáticamente en Eureka al iniciar:

```yaml
eureka:
  client:
    register-with-eureka: true
    fetch-registry: true
```

### Descubrimiento de Servicios

Las URIs de los servicios se resuelven dinámicamente:

```yaml
uri: lb://CATALOG-SERVICE  # Load balanced
uri: lb://USER-SERVICE     # Load balanced
```

## 🐛 Troubleshooting

### Gateway no encuentra los servicios

1. Verificar que Eureka esté corriendo
2. Verificar que los servicios estén registrados en Eureka
3. Revisar logs del gateway:
   ```bash
   docker logs api-gateway
   ```

### Circuit Breaker en estado OPEN

1. Verificar la salud del servicio downstream
2. Revisar logs del servicio afectado
3. Esperar el tiempo de recuperación (10s)

### Errores CORS

1. Verificar la configuración de `allowedOrigins`
2. Asegurar que las credenciales estén habilitadas si es necesario
3. Revisar headers en la petición del cliente

## 📞 Soporte

Para más información sobre la arquitectura general del proyecto, consultar el [README principal](../README.md).

## 📄 Licencia

Este proyecto es parte del sistema BusConnect.
