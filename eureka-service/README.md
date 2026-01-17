# Eureka Service - BusConnect

## 📋 Descripción

Servidor de descubrimiento de servicios (Service Discovery) basado en Netflix Eureka. Actúa como registro centralizado para todos los microservicios de la arquitectura BusConnect, permitiendo el descubrimiento dinámico de servicios y balanceo de carga del lado del cliente.

## 🏗️ Arquitectura

Eureka Server es el **corazón del Service Discovery** en la arquitectura de microservicios:

- **Registro de Servicios**: Los microservicios se registran automáticamente al iniciar
- **Health Checks**: Monitoreo continuo de la salud de los servicios
- **Service Discovery**: Los clientes consultan Eureka para localizar servicios
- **Load Balancing**: Distribuye peticiones entre instancias disponibles
- **Self-Preservation**: Protección contra fallos de red masivos

## 🔧 Tecnologías

- **Spring Boot**: 3.3.13
- **Spring Cloud Netflix Eureka Server**: Service Discovery
- **Spring Boot Actuator**: Health checks y métricas
- **Spring Web**: UI de gestión de Eureka

## 📦 Configuración

### Puertos

- **Puerto del servidor**: `8761`
- **UI de Eureka**: `http://localhost:8761`
- **API de Eureka**: `http://localhost:8761/eureka/`

### Configuración del Servidor

```yaml
eureka:
  client:
    register-with-eureka: false    # No se registra a sí mismo
    fetch-registry: false          # No necesita obtener el registro
  server:
    enable-self-preservation: false        # Deshabilitado en desarrollo
    eviction-interval-timer-in-ms: 5000   # Verificación cada 5s
```

### Self-Preservation Mode

**Deshabilitado en desarrollo** para detectar fallos rápidamente.

**En producción** debería estar habilitado:
```yaml
eureka:
  server:
    enable-self-preservation: true
```

⚠️ **Nota**: Self-preservation protege contra fallos de red evitando la eliminación masiva de servicios.

## 🌐 Dashboard Web

### Acceso

Abrir en el navegador: **http://localhost:8761**

### Información Disponible

- **Registered Instances**: Lista de servicios registrados
- **General Info**: Estado del servidor
- **System Status**: Información del entorno
- **DS Replicas**: Réplicas del servidor (si hay cluster)
- **Instances Currently Registered**: Instancias activas por servicio

### Ejemplo de Dashboard

```
Application          AMIs        Availability Zones    Status
API-GATEWAY          n/a (1)     (1)                   UP (1) - api-gateway:8080
CATALOG-SERVICE      n/a (1)     (1)                   UP (1) - catalog-service:8083
USER-SERVICE         n/a (1)     (1)                   UP (1) - user-service:8082
```

## 📊 Endpoints

### Service Registry

#### GET `/eureka/apps`
Obtiene todos los servicios registrados (formato XML por defecto).

```bash
curl http://localhost:8761/eureka/apps
```

#### GET `/eureka/apps/{appName}`
Obtiene información de un servicio específico.

```bash
curl http://localhost:8761/eureka/apps/CATALOG-SERVICE
```

#### GET `/eureka/apps/{appName}/{instanceId}`
Obtiene información de una instancia específica.

```bash
curl http://localhost:8761/eureka/apps/CATALOG-SERVICE/catalog-service:8083
```

### Actuator Endpoints

#### GET `/actuator/health`
Health check del servidor Eureka.

```bash
curl http://localhost:8761/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

#### GET `/actuator/info`
Información general del servicio.

```bash
curl http://localhost:8761/actuator/info
```

## 🔌 Registro de Clientes

### Configuración en Microservicios

Los microservicios deben incluir esta configuración para registrarse en Eureka:

```yaml
spring:
  application:
    name: mi-servicio

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${random.value}
```

### Dependencia Maven

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

### Anotación en Aplicación

```java
@SpringBootApplication
@EnableDiscoveryClient
public class MiServicioApplication {
    // ...
}
```

## 🐳 Docker

### Dockerfile

El servicio incluye un Dockerfile multi-stage optimizado con:

- ✅ Build multi-stage
- ✅ Usuario no-root (spring:spring)
- ✅ Health check integrado
- ✅ JVM optimizado (512MB max heap)
- ✅ Base Alpine (imagen ligera)

### Construcción de la imagen

```bash
docker build -t busconnect/eureka-service:latest .
```

### Ejecución con Docker

```bash
docker run -d \
  -p 8761:8761 \
  --name eureka-service \
  busconnect/eureka-service:latest
```

### Health Check

El contenedor incluye health check automático:

```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8761/actuator/health || exit 1
```

## 🚀 Ejecución

### Desarrollo Local

**Requisitos previos:**
- Java 21
- Maven 3.9+

**Pasos:**

```bash
# Opción 1: Maven
mvn spring-boot:run

# Opción 2: JAR
mvn clean package
java -jar target/eureka-server-0.0.1-SNAPSHOT.jar
```

### Con Docker Compose

```bash
docker-compose up eureka-service
```

## 📝 Estructura del Proyecto

```
eureka-service/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/busconnect/eurekaservice/
│       │       ├── EurekaServiceApplication.java
│       │       └── service/
│       │           ├── EurekaServiceI.java
│       │           └── EurekaService.java
│       └── resources/
│           └── application.yml
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md
```

## 🔍 Servicios Registrados

### Servicios del Ecosistema BusConnect

| Servicio | Puerto | Descripción |
|----------|--------|-------------|
| **api-gateway** | 8080 | API Gateway - Punto de entrada único |
| **catalog-service** | 8083 | Gestión de rutas y catálogo |
| **user-service** | 8082 | Gestión de usuarios |

### Verificar Servicios Registrados

```bash
# Ver todos los servicios
curl http://localhost:8761/eureka/apps | grep "<app>"

# Ver instancias de catalog-service
curl -H "Accept: application/json" \
  http://localhost:8761/eureka/apps/CATALOG-SERVICE
```

## 🔧 Configuración Avanzada

### Cluster de Eureka (Alta Disponibilidad)

Para producción, se recomienda un cluster de servidores Eureka:

**Servidor 1:**
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://eureka2:8761/eureka/,http://eureka3:8761/eureka/
  instance:
    hostname: eureka1
```

**Servidor 2:**
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://eureka1:8761/eureka/,http://eureka3:8761/eureka/
  instance:
    hostname: eureka2
```

### Configuración de Timeouts

```yaml
eureka:
  server:
    response-cache-update-interval-ms: 5000    # Actualización de caché
    eviction-interval-timer-in-ms: 5000        # Intervalo de evicción
  instance:
    lease-renewal-interval-in-seconds: 10      # Renovación de lease
    lease-expiration-duration-in-seconds: 30   # Expiración de lease
```

### Configuración de Renovación

```yaml
eureka:
  instance:
    lease-renewal-interval-in-seconds: 30     # Heartbeat cada 30s
    lease-expiration-duration-in-seconds: 90  # Expira después de 90s sin heartbeat
```

## 🔐 Seguridad

### Buenas Prácticas

- ✅ Usuario no-root en Docker
- ✅ Health checks habilitados
- ✅ Self-preservation configurable
- ✅ Actuator endpoints limitados

### Próximas Mejoras

- [ ] Autenticación básica HTTP
- [ ] HTTPS/TLS
- [ ] Autenticación de servicios con tokens
- [ ] Rate limiting
- [ ] Monitoreo con Spring Boot Admin

### Habilitar Autenticación Básica

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

```yaml
spring:
  security:
    user:
      name: admin
      password: ${EUREKA_PASSWORD}
```

## 🧪 Pruebas

### Verificar que Eureka está corriendo

```bash
curl http://localhost:8761/actuator/health
```

### Ver servicios registrados (JSON)

```bash
curl -H "Accept: application/json" \
  http://localhost:8761/eureka/apps
```

### Registrar un servicio de prueba

```bash
curl -X POST \
  http://localhost:8761/eureka/apps/TEST-SERVICE \
  -H "Content-Type: application/json" \
  -d '{
    "instance": {
      "hostName": "localhost",
      "app": "TEST-SERVICE",
      "ipAddr": "127.0.0.1",
      "port": {"$": 9999, "@enabled": true},
      "status": "UP",
      "dataCenterInfo": {
        "@class": "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
        "name": "MyOwn"
      }
    }
  }'
```

### Eliminar registro de prueba

```bash
curl -X DELETE \
  http://localhost:8761/eureka/apps/TEST-SERVICE/localhost:TEST-SERVICE:9999
```

## 📊 Monitoreo

### Logs

```bash
# Ver logs en Docker
docker logs -f eureka-service

# Ver logs con fecha
docker logs --since 30m eureka-service
```

### Métricas Importantes

- **Registered Replicas**: Número de réplicas registradas
- **Current Time**: Hora actual del servidor
- **Uptime**: Tiempo de actividad
- **Renewal Threshold**: Umbral de renovaciones por minuto
- **Renews (last min)**: Renovaciones en el último minuto

### Estado de Self-Preservation

Visible en el dashboard web:

```
EMERGENCY! EUREKA MAY BE INCORRECTLY CLAIMING INSTANCES ARE UP WHEN THEY'RE NOT.
RENEWALS ARE LESSER THAN THRESHOLD AND HENCE THE INSTANCES ARE NOT BEING EXPIRED JUST TO BE SAFE.
```

⚠️ Aparece cuando el modo self-preservation está activo.

## 🐛 Troubleshooting

### Servicio no aparece en Eureka

**Posibles causas:**
1. URL de Eureka incorrecta en el cliente
2. Cliente no tiene `@EnableDiscoveryClient`
3. Cliente no puede alcanzar Eureka (red/firewall)
4. Self-preservation activo (no expira servicios)

**Solución:**
```bash
# Verificar logs del cliente
docker logs mi-servicio

# Verificar conectividad
curl http://localhost:8761/actuator/health

# Verificar configuración del cliente
cat config/application.yml
```

### Servicio aparece como DOWN

**Causas:**
- Health check del servicio falla
- Servicio no responde a heartbeats

**Solución:**
```bash
# Verificar health del servicio
curl http://localhost:8083/actuator/health

# Revisar logs
docker logs catalog-service
```

### Instancias duplicadas

**Causa:** `instance-id` no es único

**Solución:**
```yaml
eureka:
  instance:
    instance-id: ${spring.application.name}:${random.value}
```

### Self-Preservation Mode Activado

**Mensaje:** "EMERGENCY! EUREKA MAY BE INCORRECTLY..."

**Causa:** Muchos servicios dejaron de enviar heartbeats

**Solución:**
- En **desarrollo**: Desactivar self-preservation
- En **producción**: Investigar problemas de red

## 📈 Optimización

### Reducir Tiempo de Detección de Fallos

```yaml
eureka:
  server:
    eviction-interval-timer-in-ms: 1000  # Verificar cada 1s
  instance:
    lease-renewal-interval-in-seconds: 5       # Heartbeat cada 5s
    lease-expiration-duration-in-seconds: 10   # Expirar después de 10s
```

⚠️ **Advertencia**: Valores muy bajos aumentan la carga de red.

### Optimizar JVM

```bash
# Configuración de memoria
JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# En Docker
docker run -e JAVA_OPTS="-Xms256m -Xmx512m" eureka-service
```

## 🔄 Integración con Otros Servicios

### API Gateway

El API Gateway usa Eureka para descubrir servicios:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: catalog-service
          uri: lb://CATALOG-SERVICE  # lb = load balanced via Eureka
```

### Microservicios

Los microservicios se registran automáticamente:

```java
@SpringBootApplication
@EnableDiscoveryClient
public class CatalogServiceApplication {
    // Se registra automáticamente en Eureka
}
```

## 📞 Comunicación entre Servicios

### Con RestTemplate

```java
@LoadBalanced
@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}

// Usar nombre del servicio en lugar de URL
restTemplate.getForObject("http://CATALOG-SERVICE/api/routes", Routes.class);
```

### Con WebClient (Reactivo)

```java
@LoadBalanced
@Bean
public WebClient.Builder webClientBuilder() {
    return WebClient.builder();
}

// Llamada reactiva
webClient.get()
    .uri("http://USER-SERVICE/api/users/{id}", userId)
    .retrieve()
    .bodyToMono(User.class);
```

## 📚 Referencias

- [Netflix Eureka Wiki](https://github.com/Netflix/eureka/wiki)
- [Spring Cloud Netflix](https://spring.io/projects/spring-cloud-netflix)
- [Service Discovery Pattern](https://microservices.io/patterns/service-registry.html)

## 📄 Licencia

Este proyecto es parte del sistema BusConnect.

## 🤝 Soporte

Para más información sobre la arquitectura general del proyecto, consultar el [README principal](../README.md).
