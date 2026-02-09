# Eureka Service - BusConnect
## Documento Tecnico

---

## 1. Resumen Tecnico

### 1.1 Que es este Eureka Service
Eureka Service es el servidor de descubrimiento de servicios (Service Discovery) del ecosistema BusConnect. Implementado con Spring Cloud Netflix Eureka Server sobre Spring Boot, actua como un registro centralizado donde todos los microservicios se registran automaticamente al iniciar y pueden descubrir otros servicios de forma dinamica.

### 1.2 Rol exacto dentro de BusConnect
- **Registro centralizado de servicios**: Todos los microservicios se registran al iniciar enviando su metadata (IP, puerto, nombre, estado)
- **Descubrimiento dinamico**: Permite a los microservicios localizar otros servicios sin necesidad de conocer IPs o puertos fijos
- **Monitoreo de salud**: Verifica continuamente la disponibilidad de cada servicio mediante heartbeats periodicos (cada 30 segundos)
- **Facilitador de balanceo de carga**: Proporciona la lista de instancias disponibles a los clientes para que implementen balanceo de carga client-side (via Ribbon)
- **Tolerancia a fallos**: Implementa modo de autopreservacion para evitar expulsiones masivas durante particiones de red
- **Dashboard de visualizacion**: Provee interfaz web en el puerto `8761` para monitorear el estado de todos los servicios registrados

---

## 2. Alcance Funcional

### 2.1 Responsabilidades (que hace)
- Aceptar y almacenar registros de microservicios que se conectan via REST API
- Mantener un registro en memoria de todas las instancias activas de cada servicio
- Recibir y procesar heartbeats de cada instancia registrada (cada 30 segundos)
- Expulsar instancias que no envien heartbeat dentro del periodo de lease (90 segundos)
- Verificar instancias caidas cada 5 segundos (eviction interval)
- Proveer API REST para consultar servicios registrados (`/eureka/apps`)
- Exponer dashboard web HTML en la raiz (`http://localhost:8761`)
- Exponer endpoints de Actuator para monitoreo externo (`/actuator/health`, `/actuator/info`)
- Replicar registros entre nodos Eureka en configuraciones cluster (no implementado actualmente)

### 2.2 Fuera de alcance (que NO hace)
- **NO implementa logica de negocio**: No procesa datos de dominio (usuarios, rutas, empresas)
- **NO persiste datos en disco**: El registro de servicios se mantiene unicamente en memoria
- **NO realiza balanceo de carga**: Solo provee la lista de instancias; el balanceo lo hace el cliente (Ribbon)
- **NO enruta peticiones**: No actua como proxy; eso es responsabilidad del API Gateway
- **NO realiza autenticacion/autorizacion**: No hay implementacion de seguridad JWT/OAuth2 (detectado como limitacion actual)
- **NO gestiona configuracion**: No es un Config Server; cada servicio gestiona su propia configuracion
- **NO monitorea metricas de negocio**: Solo monitorea disponibilidad (UP/DOWN) de instancias
- **NO transforma datos**: No modifica ni intercepta comunicaciones entre servicios

---

## 3. Estructura del Proyecto

### 3.1 Arbol de directorios
```
eureka-service/
├── src/main/
│   ├── java/com/busconnect/eurekaservice/
│   │   ├── EurekaServiceApplication.java          # Punto de entrada
│   │   └── service/
│   │       ├── EurekaService.java                 # Interfaz (comentada)
│   │       └── EurekaServiceI.java                # Implementacion (comentada)
│   └── resources/
│       └── application.yml                        # Configuracion principal
├── target/                                        # Artefactos compilados
├── Dockerfile                                     # Imagen Docker multi-stage
├── docker-compose.yml                             # Definicion Docker Compose
├── pom.xml                                        # Dependencias Maven
├── ARCHITECTURE.md                                # Diagramas de arquitectura
├── README.md                                      # Documentacion funcional
└── TECHNICAL_DOCUMENTATION.md                     # Este documento
```

### 3.2 Componentes principales

#### EurekaServiceApplication.java
**Ubicacion**: [src/main/java/com/busconnect/eurekaservice/EurekaServiceApplication.java](src/main/java/com/busconnect/eurekaservice/EurekaServiceApplication.java)

**Rol**: Clase bootstrap de Spring Boot y activacion del servidor Eureka.

**Anotaciones relevantes**:
- `@SpringBootApplication`: Configuracion automatica de Spring Boot
- `@EnableEurekaServer`: Habilita este aplicativo como servidor de registro Eureka

**Contenido**:
```java
package com.busconnect.eurekaservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class EurekaServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServiceApplication.class, args);
    }
}
```

**Analisis**:
- `@SpringBootApplication`: Meta-anotacion que habilita escaneo de componentes, auto-configuracion y soporte de propiedades
- `@EnableEurekaServer`: Anotacion critica que activa la auto-configuracion completa del servidor Eureka, incluyendo el dashboard web, la REST API de registro, el mecanismo de heartbeat y el sistema de expulsion
- `main()`: Punto de entrada estandar que bootstraps la aplicacion Spring Boot
- No se requieren clases adicionales de configuracion: toda la funcionalidad es proporcionada automaticamente por `spring-cloud-starter-netflix-eureka-server`

#### EurekaService.java (INACTIVO)
**Ubicacion**: [src/main/java/com/busconnect/eurekaservice/service/EurekaService.java](src/main/java/com/busconnect/eurekaservice/service/EurekaService.java)

**Estado**: Completamente comentado.

```java
//package com.busconnect.eurekaservice.service;
//
//public interface EurekaService {
//}
```

**Nota**: Interfaz placeholder sin metodos definidos. No tiene funcionalidad activa.

#### EurekaServiceI.java (INACTIVO)
**Ubicacion**: [src/main/java/com/busconnect/eurekaservice/service/EurekaServiceI.java](src/main/java/com/busconnect/eurekaservice/service/EurekaServiceI.java)

**Estado**: Completamente comentado.

```java
//package com.busconnect.eurekaservice.service;
//
//import com.busconnect.eurekaservice.EurekaServiceApplication;
//
//public class EurekaServiceI implements EurekaServiceApplication {
//}
```

**Nota**: Implementacion placeholder. Ademas, la firma `implements EurekaServiceApplication` seria incorrecta ya que `EurekaServiceApplication` no es una interfaz. Estas clases parecen ser codigo legacy o scaffolding inicial que nunca fue completado.

#### application.yml
**Ubicacion**: [src/main/resources/application.yml](src/main/resources/application.yml)

**Rol**: Configuracion centralizada del servidor Eureka, incluyendo puerto, comportamiento de registro, autopreservacion y endpoints de Actuator.

**Secciones clave**:
- `server.port`: Puerto de escucha del servidor
- `spring.application.name`: Nombre del servicio
- `eureka.client`: Comportamiento como cliente Eureka (deshabilitado)
- `eureka.server`: Parametros del servidor (autopreservacion, expulsion)
- `management.endpoints`: Configuracion de Actuator

#### Dockerfile
**Ubicacion**: [Dockerfile](Dockerfile)

**Rol**: Define imagen Docker optimizada con multi-stage build (2 stages).

**Stages**:
1. **builder**: Descarga dependencias y compila la aplicacion con Maven
2. **runtime**: Imagen JRE minima con usuario no-root y health check

#### docker-compose.yml
**Ubicacion**: [docker-compose.yml](docker-compose.yml)

**Rol**: Define el servicio Eureka dentro del ecosistema Docker Compose de BusConnect.

---

## 4. Entradas al Sistema

### 4.1 Endpoints del registro de servicios (Eureka REST API)

#### Registro de instancia
**Endpoint**: `POST /eureka/apps/{appName}`
**Proposito**: Registrar una nueva instancia de un microservicio.
**Invocado por**: Cada microservicio al iniciar (automatico via Eureka Client).

**Payload de ejemplo**:
```xml
<instance>
  <instanceId>api-gateway:random-value</instanceId>
  <hostName>api-gateway</hostName>
  <app>API-GATEWAY</app>
  <ipAddr>172.18.0.3</ipAddr>
  <status>UP</status>
  <port enabled="true">8080</port>
  <dataCenterInfo>
    <name>MyOwn</name>
  </dataCenterInfo>
</instance>
```

**Respuesta exitosa**: `204 No Content`

#### Heartbeat (renovacion de lease)
**Endpoint**: `PUT /eureka/apps/{appName}/{instanceId}`
**Proposito**: Renovar el lease de una instancia registrada.
**Frecuencia**: Cada 30 segundos (configurable por el cliente).
**Respuesta exitosa**: `200 OK`
**Si la instancia no existe**: `404 Not Found` (el cliente se re-registra automaticamente)

#### Consultar todos los servicios
**Endpoint**: `GET /eureka/apps`
**Proposito**: Obtener la lista completa de servicios y sus instancias.
**Formato de respuesta**: XML (por defecto) o JSON (con header `Accept: application/json`)
**Invocado por**: Clientes Eureka cada 30 segundos para actualizar cache local.

**Respuesta de ejemplo**:
```json
{
  "applications": {
    "application": [
      {
        "name": "API-GATEWAY",
        "instance": [
          {
            "instanceId": "api-gateway:abc123",
            "hostName": "api-gateway",
            "app": "API-GATEWAY",
            "ipAddr": "172.18.0.3",
            "status": "UP",
            "port": {"$": 8080, "@enabled": "true"}
          }
        ]
      },
      {
        "name": "USER-SERVICE",
        "instance": [...]
      }
    ]
  }
}
```

#### Consultar servicio especifico
**Endpoint**: `GET /eureka/apps/{appName}`
**Proposito**: Obtener informacion de todas las instancias de un servicio.
**Ejemplo**: `GET /eureka/apps/USER-SERVICE`

#### Consultar instancia especifica
**Endpoint**: `GET /eureka/apps/{appName}/{instanceId}`
**Proposito**: Obtener informacion detallada de una instancia particular.

#### Dar de baja instancia
**Endpoint**: `DELETE /eureka/apps/{appName}/{instanceId}`
**Proposito**: Eliminar una instancia del registro.
**Invocado por**: Microservicio al hacer shutdown graceful.
**Respuesta exitosa**: `200 OK`

#### Cambiar estado de instancia
**Endpoint**: `PUT /eureka/apps/{appName}/{instanceId}/status?value={status}`
**Proposito**: Cambiar el estado de una instancia (UP, DOWN, OUT_OF_SERVICE).
**Uso**: Mantenimiento programado de servicios.

### 4.2 Endpoints propios del servidor

#### Dashboard Web
- `GET /` → [http://localhost:8761](http://localhost:8761) - Panel principal de Eureka (interfaz HTML)

**Informacion mostrada en el dashboard**:
- Estado del sistema (Environment, Data center)
- Instancias actualmente registradas (nombre, AMIs, zonas, status)
- Informacion general (total de instancias, renovaciones por minuto, umbral)
- Estado de autopreservacion (activado/desactivado)
- Replicas configuradas (en modo cluster)

#### Actuator Endpoints
- `GET /actuator/health` → [FallbackController.java:17-27](src/main/java/com/busconnect/apigateway/controller/FallbackController.java#L17-L27) Health check completo
- `GET /actuator/info` → Informacion de la aplicacion

**Configuracion**: [application.yml:17-22](src/main/resources/application.yml#L17-L22)

---

## 5. Flujo de Registro y Descubrimiento

### 5.1 Flujo completo paso a paso

#### Ejemplo: Registro del `USER-SERVICE` al iniciar

**Paso 1: Inicio del microservicio**
- `USER-SERVICE` inicia en el puerto `8082`
- Spring Boot detecta dependencia `spring-cloud-starter-netflix-eureka-client`
- `@EnableDiscoveryClient` activa el cliente Eureka
- Configuracion: `eureka.client.service-url.defaultZone: http://localhost:8761/eureka/`

**Paso 2: Registro inicial en Eureka**
- **Delay inicial**: ~40 segundos despues del inicio (`initial-instance-info-replication-interval-seconds: 40`)
- **Endpoint llamado**: `POST /eureka/apps/USER-SERVICE`
- **Payload**: Metadata de la instancia (IP, puerto, hostname, status UP)
- **Respuesta de Eureka**: `204 No Content` (registro exitoso)

**Paso 3: Eureka almacena la instancia**
- Eureka crea una entrada en su registro en memoria
- Asigna un **lease** con duracion de 90 segundos
- La instancia aparece en el dashboard web como `UP`
- Eureka incrementa el contador de renovaciones esperadas

**Paso 4: Heartbeat periodico**
- Cada **30 segundos**, `USER-SERVICE` envia: `PUT /eureka/apps/USER-SERVICE/{instanceId}`
- Eureka actualiza el timestamp del ultimo heartbeat
- Eureka responde: `200 OK`
- Si Eureka responde `404 Not Found`, el cliente se re-registra automaticamente

**Paso 5: Fetch del registro por otros servicios**
- `API-GATEWAY` consulta: `GET /eureka/apps` (cada 30 segundos)
- Eureka responde con la lista completa de servicios registrados
- `API-GATEWAY` actualiza su cache local con las instancias de `USER-SERVICE`
- El cache permite que el gateway siga funcionando si Eureka esta temporalmente caido

**Paso 6: Descubrimiento por el API Gateway**
- Cuando llega una peticion `POST /api/users` al gateway
- Gateway busca `USER-SERVICE` en su cache local (obtenido de Eureka)
- Ribbon selecciona una instancia usando Round Robin
- Gateway hace forward HTTP a `http://user-service:8082/api/users`

### 5.2 Flujo de expulsion de instancia caida

**Paso 1: Servicio deja de enviar heartbeats**
- `USER-SERVICE` se cae o pierde conectividad
- Ultimo heartbeat recibido: `T0`

**Paso 2: Verificacion periodica de Eureka**
- Eureka ejecuta `EvictionTask` cada **5 segundos** (`eviction-interval-timer-in-ms: 5000`)
- En cada ejecucion, revisa todas las instancias registradas
- Calcula: `tiempo_actual - ultimo_heartbeat > lease_duration (90s)?`

**Paso 3: Expulsion (T0 + 90 segundos)**
- Eureka determina que la instancia ha expirado
- Elimina la instancia del registro
- La instancia desaparece del dashboard web
- Eureka decrementa el contador de renovaciones esperadas

**Paso 4: Propagacion a clientes**
- En el proximo fetch del registro (maximo 30 segundos despues)
- `API-GATEWAY` recibe registro actualizado sin la instancia caida
- Gateway deja de enrutar peticiones a esa instancia

**Paso 5: Impacto en peticiones**
- Peticiones a `USER-SERVICE` activan el Circuit Breaker del gateway
- Si la tasa de fallos supera el 50%, el circuit breaker se abre
- Peticiones subsiguientes reciben respuesta de fallback (503)

### 5.3 Flujo alternativo: Autopreservacion

**Nota**: La autopreservacion esta **deshabilitada** en la configuracion actual (`enable-self-preservation: false`). Este flujo aplica cuando se habilita para produccion.

**Paso 1: Deteccion de renovaciones bajas**
- Eureka calcula la tasa de renovaciones recibidas vs esperadas
- Umbral: **85%** de las renovaciones esperadas

**Paso 2: Activacion del modo de autopreservacion**
- Si la tasa cae por debajo del 85%, Eureka **deja de expulsar instancias**
- El dashboard muestra: `"EMERGENCY! EUREKA MAY BE INCORRECTLY CLAIMING INSTANCES ARE UP WHEN THEY'RE NOT."`
- **Razon**: Eureka asume que el problema es de red (particion), no de los servicios

**Paso 3: Comportamiento durante autopreservacion**
- Instancias que no envian heartbeat **NO son expulsadas**
- Nuevos registros y heartbeats siguen siendo procesados normalmente
- El dashboard muestra la advertencia de emergencia

**Paso 4: Desactivacion automatica**
- Cuando la tasa de renovaciones vuelve a superar el 85%
- Eureka reanuda la expulsion normal de instancias expiradas
- La advertencia desaparece del dashboard

---

## 6. Configuracion

### 6.1 application.yml

#### Servidor
**Lineas**: [application.yml:1-2](src/main/resources/application.yml#L1-L2)
```yaml
server:
  port: 8761
```
**Descripcion**: Puerto estandar de Eureka Server. Convencion adoptada por la industria para servidores de descubrimiento Netflix Eureka.

#### Aplicacion
**Lineas**: [application.yml:4-6](src/main/resources/application.yml#L4-L6)
```yaml
spring:
  application:
    name: eureka-server
```
**Descripcion**: Nombre identificador del servicio. Usado internamente por Spring Boot y visible en los logs.

#### Eureka Client (deshabilitado)
**Lineas**: [application.yml:8-11](src/main/resources/application.yml#L8-L11)
```yaml
eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

**Descripcion**:
- `register-with-eureka: false`: El servidor Eureka **no se registra a si mismo** como servicio. En un despliegue con un unico nodo, no tiene sentido que el servidor se auto-registre.
- `fetch-registry: false`: El servidor **no descarga el registro** de otros nodos Eureka. Solo aplica en configuraciones single-node; en cluster se pondria `true` para replicacion.

**Implicacion**: Estas dos propiedades son **criticas**. Si no se configuran como `false` en un nodo unico, Eureka intentaria registrarse consigo mismo y lanzaria excepciones en los logs al no encontrar otro nodo peer.

#### Eureka Server
**Lineas**: [application.yml:12-15](src/main/resources/application.yml#L12-L15)
```yaml
eureka:
  server:
    enable-self-preservation: false
    eviction-interval-timer-in-ms: 5000
```

**Parametros explicados**:

| Parametro | Valor | Descripcion |
|-----------|-------|-------------|
| `enable-self-preservation` | `false` | Deshabilitado para deteccion rapida de fallos en desarrollo. En produccion deberia ser `true` |
| `eviction-interval-timer-in-ms` | `5000` | Frecuencia de verificacion de instancias expiradas (cada 5 segundos). Valor por defecto: 60000ms (60s) |

**Analisis de la decision**:
- **Autopreservacion deshabilitada**: Permite que instancias caidas se eliminen inmediatamente del registro, lo que es ideal para desarrollo donde servicios se reinician frecuentemente. En produccion, habilitarla previene expulsiones masivas durante particiones de red temporales.
- **Eviction a 5 segundos**: 12x mas rapido que el valor por defecto (60s). Permite deteccion casi inmediata de servicios caidos en desarrollo, pero genera mas overhead de procesamiento.

#### Actuator
**Lineas**: [application.yml:17-22](src/main/resources/application.yml#L17-L22)
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always
```

**Descripcion**:
- Expone unicamente endpoints de `health` e `info` (seguridad por defecto minimo)
- `show-details: always`: Muestra detalles completos del estado de salud (diskSpace, ping, etc.)
- Endpoints como `env`, `beans`, `metrics` estan **excluidos** intencionalmente

### 6.2 Variables de entorno

#### EUREKA_CLIENT_SERVICEURL_DEFAULTZONE (en clientes)
**Uso**: Los microservicios cliente configuran esta variable para apuntar al servidor Eureka.

**Valores segun entorno**:
- **Local**: `http://localhost:8761/eureka/`
- **Docker**: `http://eureka-service:8761/eureka/` (usando nombre del servicio Docker)
- **Cluster**: `http://eureka1:8761/eureka/,http://eureka2:8761/eureka/` (multiples nodos)

#### SPRING_PROFILES_ACTIVE
**Uso**: Activa perfiles de configuracion.
**Valores posibles**:
- `default`: Configuracion local
- `docker`: Configuracion para contenedores

### 6.3 Configuracion de Docker

#### Dockerfile - Multi-stage build
**Ubicacion**: [Dockerfile](Dockerfile)

**Stage 1 - builder**:
```dockerfile
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copia POMs del proyecto padre y modulos para resolucion de dependencias
COPY pom.xml .
COPY eureka-service/pom.xml eureka-service/
COPY user-service/pom.xml user-service/
COPY catalog-service/pom.xml catalog-service/
COPY api-gateway/pom.xml api-gateway/

# Descarga dependencias (capa cacheada)
RUN mvn dependency:go-offline -pl eureka-service -am -B

# Copia codigo fuente y compila
COPY eureka-service/src eureka-service/src
RUN mvn clean package -pl eureka-service -am -DskipTests -B
```

**Proposito**:
- Imagen base: `maven:3.9-eclipse-temurin-21-alpine` (incluye Maven 3.9 y JDK 21)
- Cachea dependencias Maven en una capa separada (solo se re-descarga si cambia `pom.xml`)
- Compila el modulo `eureka-service` y sus dependencias padre (`-am`)
- Omite tests (`-DskipTests`) para acelerar el build

**Stage 2 - runtime**:
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Crear usuario no-root
RUN addgroup -g 1001 -S spring && \
    adduser -u 1001 -S spring -G spring

# Copiar JAR desde builder
COPY --from=builder /app/eureka-service/target/*.jar app.jar

# Cambiar ownership
RUN chown spring:spring app.jar

USER spring

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8761/actuator/health || exit 1

EXPOSE 8761
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**Proposito**:
- Imagen base: `eclipse-temurin:21-jre-alpine` (solo JRE, sin JDK ni Maven - imagen ligera ~80MB)
- Usuario no-root `spring:spring` (UID/GID 1001) por seguridad
- Health check via `wget` al endpoint de Actuator
- JVM configurada con heap de 256MB-512MB

#### Optimizaciones JVM

| Parametro | Valor | Descripcion |
|-----------|-------|-------------|
| `-Xms` | `256MB` | Heap inicial de la JVM |
| `-Xmx` | `512MB` | Heap maximo de la JVM |

**Nota**: A diferencia del API Gateway, el Dockerfile de Eureka no usa flags avanzados como `UseContainerSupport` o `MaxRAMPercentage`. Esto podria mejorarse para despliegues en contenedores con limites de memoria.

#### Health Check del contenedor

| Parametro | Valor | Descripcion |
|-----------|-------|-------------|
| `--interval` | 30s | Intervalo entre verificaciones |
| `--timeout` | 10s | Tiempo maximo de espera por respuesta |
| `--start-period` | 60s | Tiempo de gracia al iniciar (startup de Spring Boot + Eureka) |
| `--retries` | 3 | Numero de fallos antes de marcar como unhealthy |

**Herramienta**: `wget` (disponible en Alpine por defecto, a diferencia de `curl` que requiere instalacion)

#### docker-compose.yml

```yaml
eureka-server:
  build: ./eureka-server
  container_name: eureka-server
  ports:
    - "8761:8761"
  networks:
    - busconnect-net
```

| Propiedad | Valor | Descripcion |
|-----------|-------|-------------|
| Nombre del servicio | `eureka-server` | Identificador en Docker Compose |
| Build context | `./eureka-server` | Directorio del Dockerfile |
| Puerto | `8761:8761` | Mapeo host:contenedor |
| Red | `busconnect-net` | Red personalizada compartida entre microservicios |

**Nota importante**: Existe una inconsistencia entre el nombre del directorio (`eureka-service`) y la referencia en docker-compose (`./eureka-server`). Esto podria causar errores al ejecutar `docker-compose build`. Debe corregirse a `./eureka-service`.

---

## 7. Integracion con otros Microservicios

### 7.1 Servicios que se registran en Eureka

#### API-GATEWAY
**Nombre en Eureka**: `API-GATEWAY`
**Puerto**: 8080
**Hostname Docker**: `api-gateway`

**Configuracion del cliente** (en api-gateway):
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
    registry-fetch-interval-seconds: 30
  instance:
    prefer-ip-address: true
    lease-renewal-interval-in-seconds: 30
    lease-expiration-duration-in-seconds: 90
```

**Operaciones hacia Eureka**:
1. **Registro**: `POST /eureka/apps/API-GATEWAY` (al iniciar)
2. **Heartbeat**: `PUT /eureka/apps/API-GATEWAY/{instanceId}` (cada 30s)
3. **Fetch registry**: `GET /eureka/apps` (cada 30s, para descubrir otros servicios)

**Proposito del registro**: El gateway necesita conocer las ubicaciones de `USER-SERVICE` y `CATALOG-SERVICE` para enrutar peticiones.

#### USER-SERVICE
**Nombre en Eureka**: `USER-SERVICE`
**Puerto**: 8082
**Hostname Docker**: `user-service`

**Operaciones hacia Eureka**:
1. **Registro**: `POST /eureka/apps/USER-SERVICE` (al iniciar)
2. **Heartbeat**: `PUT /eureka/apps/USER-SERVICE/{instanceId}` (cada 30s)

**Proposito del registro**: Permitir que el API Gateway descubra y enrute peticiones hacia `/api/users/**`.

#### CATALOG-SERVICE
**Nombre en Eureka**: `CATALOG-SERVICE`
**Puerto**: 8083
**Hostname Docker**: `catalog-service`

**Operaciones hacia Eureka**:
1. **Registro**: `POST /eureka/apps/CATALOG-SERVICE` (al iniciar)
2. **Heartbeat**: `PUT /eureka/apps/CATALOG-SERVICE/{instanceId}` (cada 30s)

**Proposito del registro**: Permitir que el API Gateway descubra y enrute peticiones hacia `/api/routes/**`, `/api/companies/**`, `/api/companies/buses/**`, `/api/companies/drivers/**`.

### 7.2 Diagrama de comunicacion

```
┌─────────────────────────────────────────────────────────────┐
│                    EUREKA SERVER (:8761)                     │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              Registro en Memoria                     │    │
│  │                                                     │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────┐│    │
│  │  │ API-GATEWAY  │  │ USER-SERVICE │  │  CATALOG   ││    │
│  │  │ :8080   [UP] │  │ :8082  [UP]  │  │ :8083 [UP] ││    │
│  │  └──────────────┘  └──────────────┘  └────────────┘│    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  Dashboard: http://localhost:8761                            │
│  API: /eureka/apps                                          │
│  Health: /actuator/health                                   │
└──────────┬──────────────────┬──────────────────┬────────────┘
           │                  │                  │
    Heartbeat 30s      Heartbeat 30s      Heartbeat 30s
    + Fetch registry
           │                  │                  │
    ┌──────┴──────┐    ┌──────┴──────┐    ┌──────┴──────┐
    │ API-GATEWAY │    │USER-SERVICE │    │  CATALOG    │
    │   (:8080)   │    │   (:8082)   │    │  SERVICE    │
    │             │    │             │    │   (:8083)   │
    │ Usa Eureka  │    │ Se registra │    │ Se registra │
    │ para des-   │    │ y envia     │    │ y envia     │
    │ cubrir      │    │ heartbeats  │    │ heartbeats  │
    │ servicios   │    │             │    │             │
    └─────────────┘    └─────────────┘    └─────────────┘
```

### 7.3 Protocolo de comunicacion

**Tipo**: HTTP REST (Eureka REST API)

**Cliente HTTP usado por microservicios**: `RestTemplate` (incluido en el starter de Eureka Client)

**Cache local**: Cada microservicio cliente mantiene un cache local del registro de servicios:
- Se actualiza cada 30 segundos (`registry-fetch-interval-seconds`)
- Si Eureka esta caido, el cliente usa la ultima version del cache
- Esto permite operacion degradada durante caidas temporales de Eureka

**Formato de datos**: XML (por defecto en la comunicacion Eureka) o JSON (con header `Accept: application/json`)

---

## 8. Seguridad

### 8.1 Estado actual: NO implementada

**Evidencia**:
- No hay dependencias de `spring-cloud-starter-security` en [pom.xml](pom.xml)
- No hay clases de configuracion de seguridad en `src/main/java/`
- No hay filtros de autenticacion
- El dashboard y la API REST son accesibles sin credenciales

### 8.2 Implicaciones tecnicas

**Riesgos actuales**:
- Cualquier cliente puede registrar servicios falsos (service spoofing)
- Cualquier cliente puede consultar el registro y obtener IPs/puertos internos
- Cualquier cliente puede dar de baja instancias legitimas (DELETE)
- El dashboard expone informacion de infraestructura (IPs, puertos, hostnames)
- No hay cifrado TLS en las comunicaciones Eureka

**Mitigaciones actuales**:
- Despliegue en red interna (Docker network `busconnect-net`)
- Endpoints de Actuator limitados a `health` e `info`
- El servidor no se registra a si mismo (reduce superficie de ataque)

### 8.3 Seguridad de infraestructura

#### Dockerfile
**Usuario no-root**: [Dockerfile:36-37](Dockerfile#L36-L37)
```dockerfile
RUN addgroup -g 1001 -S spring && adduser -u 1001 -S spring -G spring
USER spring
```
**Proposito**: Previene escalacion de privilegios si el contenedor es comprometido. El proceso Java se ejecuta con permisos limitados.

#### Red Docker
**Red**: `busconnect-net` (red personalizada Docker)
**Aislamiento**: Los servicios dentro de la red pueden comunicarse entre si, pero no son accesibles desde fuera del host a menos que se expongan puertos explicitamente.

### 8.4 Recomendaciones de seguridad para produccion

| Mejora | Prioridad | Descripcion |
|--------|-----------|-------------|
| Autenticacion HTTP Basic | Alta | Proteger la API de registro con usuario/password |
| HTTPS/TLS | Alta | Cifrar comunicaciones entre servicios y Eureka |
| Network policies | Alta | Restringir acceso al puerto 8761 solo desde microservicios |
| Dashboard restringido | Media | Proteger el dashboard con autenticacion o deshabilitarlo |
| Tokens entre servicios | Media | Validar identidad de servicios que se registran |
| Audit logging | Baja | Registrar operaciones de registro/baja de servicios |

---

## 9. Observabilidad y Manejo de Errores

### 9.1 Health Check

#### /actuator/health
**Metodo**: GET
**Proposito**: Health check completo del servidor Eureka.

**Respuesta exitosa**:
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 107374182400,
        "free": 53687091200,
        "threshold": 10485760,
        "path": "/app/.",
        "exists": true
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

**Status posibles**:
- `UP`: Servidor funcionando correctamente
- `DOWN`: Componente critico fallando (ej: disco lleno)
- `OUT_OF_SERVICE`: Servicio deshabilitado manualmente

**Uso**: Verificado por Docker HEALTHCHECK cada 30 segundos y potencialmente por orquestadores como Kubernetes.

#### /actuator/info
**Metodo**: GET
**Proposito**: Informacion general de la aplicacion.
**Respuesta**: Vacia por defecto (se puede enriquecer con propiedades `info.*` en application.yml).

### 9.2 Dashboard Web de Eureka

**URL**: `http://localhost:8761`

**Informacion mostrada**:

| Seccion | Datos |
|---------|-------|
| System Status | Environment, Data Center, Current time, Uptime |
| DS Replicas | Nodos peer configurados (vacio en single-node) |
| Instances currently registered | Tabla con nombre, AMIs, availability zones, status |
| General Info | Total instances, renewals/min, renewal threshold, self-preservation status |

**Alertas del dashboard**:
- **Self-preservation OFF**: `"RENEWALS ARE LESSER THAN THE THRESHOLD. THE SELF PRESERVATION MODE IS TURNED OFF. THIS MAY NOT PROTECT INSTANCE EXPIRY IN CASE OF NETWORK/OTHER PROBLEMS."`
- **Self-preservation ON (emergencia)**: `"EMERGENCY! EUREKA MAY BE INCORRECTLY CLAIMING INSTANCES ARE UP WHEN THEY'RE NOT. RENEWALS ARE LESSER THAN THRESHOLD AND HENCE THE INSTANCES ARE NOT BEING EXPIRED JUST TO BE SAFE."`

### 9.3 Logging

**Configuracion por defecto**: Spring Boot configura Logback automaticamente.

**Logs clave del servidor Eureka**:

```
# Inicio del servidor
INFO  EurekaServiceApplication : Started EurekaServiceApplication in 5.234 seconds

# Registro de instancia
INFO  AbstractInstanceRegistry : Registered instance USER-SERVICE/user-service:random-value with status UP

# Heartbeat recibido
DEBUG AbstractInstanceRegistry : Renewing [USER-SERVICE/user-service:random-value]

# Expulsion de instancia
WARN  AbstractInstanceRegistry : DS: Registry: expired lease for USER-SERVICE/user-service:random-value

# Self-preservation activada
WARN  AbstractInstanceRegistry : Self-preservation mode is turned off. This may not protect instance expiry in case of network/other problems.
```

**Recomendacion**: Para produccion, configurar formato JSON y enviar a un agregador de logs (ELK, Splunk, CloudWatch).

### 9.4 Metricas de Eureka

**Metricas internas del servidor**:

| Metrica | Descripcion |
|---------|-------------|
| Renovaciones por minuto | Heartbeats recibidos en el ultimo minuto |
| Umbral de renovaciones | Minimo esperado (85% del total) |
| Instancias registradas | Total de instancias activas |
| Instancias expiradas | Instancias eliminadas por timeout |

**Acceso**: A traves del dashboard web o via JMX (si se habilita).

---

## 10. Decisiones Tecnicas Detectadas

### 10.1 Netflix Eureka como Service Discovery

**Decision**: Usar Netflix Eureka en lugar de alternativas como Consul, Zookeeper o Kubernetes DNS.

**Evidencia**:
- Dependencia: `spring-cloud-starter-netflix-eureka-server` ([pom.xml](pom.xml))
- Anotacion: `@EnableEurekaServer` ([EurekaServiceApplication.java](src/main/java/com/busconnect/eurekaservice/EurekaServiceApplication.java))

**Implicaciones tecnicas**:
- **Ventaja**: Integracion nativa con Spring Cloud (configuracion minima)
- **Ventaja**: Amplia documentacion y comunidad
- **Ventaja**: Dashboard web integrado para monitoreo visual
- **Ventaja**: Modo de autopreservacion para redes inestables
- **Desventaja**: Eureka esta en modo de mantenimiento (Netflix dejo de desarrollar activamente)
- **Desventaja**: Registro solo en memoria (se pierde al reiniciar)
- **Desventaja**: Sin soporte nativo para key-value store o configuracion distribuida (a diferencia de Consul)

### 10.2 Single-node en lugar de cluster

**Decision**: Desplegar un unico nodo Eureka sin replicacion.

**Evidencia**:
- `register-with-eureka: false` ([application.yml:10](src/main/resources/application.yml#L10))
- `fetch-registry: false` ([application.yml:11](src/main/resources/application.yml#L11))
- No hay configuracion de `eureka.client.serviceUrl` apuntando a otros nodos

**Implicaciones tecnicas**:
- **Ventaja**: Simplicidad de despliegue y operacion
- **Ventaja**: Menor consumo de recursos (un solo servidor)
- **Desventaja**: Punto unico de fallo (SPOF) - si Eureka cae, no hay registro disponible
- **Mitigacion actual**: Los clientes mantienen cache local del registro (30s de validez)
- **Recomendacion produccion**: Cluster de 3+ nodos con replicacion bidireccional

### 10.3 Autopreservacion deshabilitada

**Decision**: Configurar `enable-self-preservation: false`.

**Evidencia**: [application.yml:13](src/main/resources/application.yml#L13)

**Implicaciones tecnicas**:
- **Ventaja desarrollo**: Servicios caidos se eliminan rapidamente del registro
- **Ventaja desarrollo**: Evita instancias "fantasma" que aparecen como UP pero estan caidas
- **Desventaja produccion**: Durante particiones de red temporales, Eureka podria expulsar instancias que siguen funcionando
- **Riesgo**: Cascada de desregistros en caso de problemas de red
- **Recomendacion**: Habilitar (`true`) en produccion

### 10.4 Eviction interval agresivo

**Decision**: Configurar `eviction-interval-timer-in-ms: 5000` (5 segundos, en lugar de 60 por defecto).

**Evidencia**: [application.yml:14](src/main/resources/application.yml#L14)

**Implicaciones tecnicas**:
- **Ventaja**: Deteccion casi inmediata de servicios caidos
- **Desventaja**: Mayor consumo de CPU (verificacion 12x mas frecuente)
- **Desventaja**: Menos tolerante a heartbeats retrasados (jitter de red)
- **Apropiado para**: Desarrollo y testing
- **Produccion**: Considerar aumentar a 30-60 segundos

### 10.5 Puerto estandar 8761

**Decision**: Usar el puerto 8761 (convencion de Netflix Eureka).

**Evidencia**: [application.yml:2](src/main/resources/application.yml#L2)

**Implicaciones tecnicas**:
- **Ventaja**: Estandar de la industria, reconocido por herramientas y documentacion
- **Ventaja**: Configuracion por defecto de Spring Cloud Eureka Client (no requiere configurar `defaultZone` si Eureka esta en localhost:8761)
- **Desventaja**: Puerto predecible (potencial vector de ataque si esta expuesto)

### 10.6 Actuator limitado

**Decision**: Exponer solo `health` e `info` en lugar de todos los endpoints.

**Evidencia**: [application.yml:20](src/main/resources/application.yml#L20)

**Implicaciones tecnicas**:
- **Ventaja seguridad**: No expone `env` (variables de entorno con posibles secretos)
- **Ventaja seguridad**: No expone `beans`, `configprops`, `mappings` (informacion de arquitectura interna)
- **Desventaja**: Menor capacidad de debugging en produccion sin acceso adicional
- **Alternativa**: Exponer mas endpoints pero con autenticacion

### 10.7 Spring Boot con starter web (no reactivo)

**Decision**: Usar `spring-boot-starter-web` (servlet-based) en lugar de `spring-boot-starter-webflux` (reactivo).

**Evidencia**: Dependencia `spring-boot-starter-web` en [pom.xml](pom.xml)

**Implicaciones tecnicas**:
- **Razon**: Eureka Server esta basado en servlets (no es compatible con WebFlux)
- **Diferencia con API Gateway**: El gateway usa WebFlux (reactivo), pero Eureka Server requiere el stack clasico
- **Implicacion**: Usa Tomcat embebido en lugar de Reactor Netty
- **Rendimiento**: Adecuado para Eureka ya que no maneja alto throughput de peticiones

### 10.8 Multi-stage Docker build de 2 stages

**Decision**: Usar 2 stages (builder + runtime) en lugar de 3 stages como el API Gateway.

**Evidencia**: [Dockerfile](Dockerfile)

**Implicaciones tecnicas**:
- **Diferencia con API Gateway**: No usa Spring Boot Layertools para extraer capas
- **Ventaja**: Dockerfile mas simple
- **Desventaja**: Cualquier cambio en el codigo reemplaza el JAR completo (no hay caching por capas)
- **Impacto**: Rebuilds ligeramente mas lentos en CI/CD comparado con layered approach
- **Justificacion**: Eureka Server raramente cambia, por lo que la optimizacion de capas es menos critica

---

## 11. Limitaciones Actuales

### 11.1 Limitaciones tecnicas

#### Sin autenticacion/autorizacion
**Evidencia**: No hay dependencias de Spring Security en [pom.xml](pom.xml).

**Impacto**:
- Cualquier cliente en la red puede registrar/desregistrar servicios
- La API REST esta completamente abierta
- El dashboard expone informacion de infraestructura sin proteccion

**Riesgo**: Service spoofing, ataques de denegacion de servicio via desregistro masivo.

#### Sin cifrado TLS
**Evidencia**: No hay configuracion de SSL en [application.yml](src/main/resources/application.yml).

**Impacto**:
- Comunicaciones entre microservicios y Eureka son en texto plano
- Heartbeats y registros pueden ser interceptados (man-in-the-middle)

#### Sin replicacion (single point of failure)
**Evidencia**: `register-with-eureka: false`, `fetch-registry: false`.

**Impacto**:
- Si el nodo Eureka cae, no hay registro alternativo
- Mitigacion parcial: cache local en clientes (30s de validez)
- Impacto real: Servicios no pueden descubrir nuevas instancias durante la caida

#### Sin persistencia
**Evidencia**: No hay configuracion de base de datos o almacenamiento.

**Impacto**:
- Al reiniciar Eureka, se pierde todo el registro
- Los servicios deben re-registrarse (delay de ~40 segundos)
- Durante el periodo de re-registro, el gateway no puede resolver servicios

#### Sin metricas detalladas
**Evidencia**: Actuator limitado a `health` e `info`.

**Impacto**:
- No se exponen metricas de rendimiento via `/actuator/metrics`
- No hay visibilidad sobre tasa de registros, heartbeats, o expulsiones via API REST
- Monitoreo limitado al dashboard web

#### Sin tests
**Evidencia**: No existen archivos de test en `src/test/`.

**Impacto**:
- No hay validacion automatizada del comportamiento del servidor
- Cambios en configuracion no se verifican en CI/CD
- Mayor riesgo de regresiones

### 11.2 Limitaciones funcionales

#### Dashboard sin personalizacion
**Evidencia**: Se usa el dashboard por defecto de Eureka.

**Limitacion**: No muestra metricas custom, graficos de tendencia, ni alertas visuales personalizadas.

#### Sin health checks activos hacia servicios
**Evidencia**: Eureka solo valida heartbeats pasivos, no verifica activamente que los servicios funcionen.

**Limitacion**: Un servicio puede enviar heartbeats pero tener su logica de negocio fallando (UP pero no funcional).

### 11.3 Limitaciones de diseño

#### Inconsistencia de nombres
**Evidencia**:
- Directorio: `eureka-service`
- Docker Compose: `eureka-server`
- Spring application name: `eureka-server`

**Impacto**: Confuso para nuevos desarrolladores, puede causar errores en scripts de CI/CD.

#### Clases comentadas sin limpiar
**Evidencia**: `EurekaService.java` y `EurekaServiceI.java` estan completamente comentadas.

**Impacto**: Codigo muerto que agrega ruido al proyecto. Deberian eliminarse o implementarse.

---

## 12. Posibles Extensiones Futuras

### 12.1 Alta disponibilidad (Cluster Eureka)

**Indicador en diseno actual**:
- Las propiedades `register-with-eureka` y `fetch-registry` estan preparadas para ser cambiadas a `true`
- Spring Cloud Eureka soporta peer-to-peer replication nativamente

**Implementacion sugerida**:
```yaml
# Nodo 1 (eureka-1)
eureka:
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      defaultZone: http://eureka-2:8761/eureka/,http://eureka-3:8761/eureka/
  instance:
    hostname: eureka-1

# Nodo 2 (eureka-2) - similar apuntando a 1 y 3
# Nodo 3 (eureka-3) - similar apuntando a 1 y 2
```

**Beneficio**: Eliminacion del SPOF, tolerancia a caida de hasta N-1 nodos.

### 12.2 Autenticacion HTTP Basic

**Indicador en diseno actual**:
- Estructura del proyecto permite agregar clases de configuracion de seguridad

**Implementacion sugerida**:

1. Agregar dependencia:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

2. Configurar seguridad:
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/eureka/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
```

3. Actualizar configuracion de clientes:
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://user:password@eureka-server:8761/eureka/
```

### 12.3 HTTPS/TLS

**Implementacion sugerida**:
```yaml
server:
  port: 8761
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

### 12.4 Spring Boot Admin

**Indicador en diseno actual**:
- Actuator ya expone endpoints de health
- Estructura de microservicios compatible con SBA

**Implementacion sugerida**:
- Agregar `spring-boot-admin-starter-server` como dependencia
- Integrar dashboard de SBA con Eureka para monitoreo centralizado
- Visualizar metricas, logs y estado de todos los microservicios desde un unico panel

### 12.5 Logging estructurado

**Implementacion sugerida**:
1. Agregar dependencia `logstash-logback-encoder`
2. Configurar `logback-spring.xml` con formato JSON
3. Enviar logs a ELK stack (Elasticsearch, Logstash, Kibana)

### 12.6 Health checks activos

**Implementacion sugerida**:
- Configurar Eureka para verificar endpoints de health de los servicios registrados
- Usar `healthCheckUrl` y `statusPageUrl` en la configuracion de instancias
- Eureka marcaria automaticamente como DOWN servicios con health check fallido

---

## 13. Dependencias

### 13.1 Tabla de dependencias directas

| Dependencia | GroupId | ArtifactId | Proposito |
|-------------|---------|------------|-----------|
| Eureka Server | `org.springframework.cloud` | `spring-cloud-starter-netflix-eureka-server` | Servidor de descubrimiento |
| Actuator | `org.springframework.boot` | `spring-boot-starter-actuator` | Endpoints de monitoreo |
| Web | `org.springframework.boot` | `spring-boot-starter-web` | Soporte HTTP/REST + Dashboard |
| Lombok | `org.projectlombok` | `lombok` (optional) | Reduccion de boilerplate |

### 13.2 Gestion de dependencias

**Spring Cloud BOM**:
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Proposito**: Garantiza compatibilidad entre todas las dependencias de Spring Cloud. Las versiones individuales no se especifican gracias al BOM.

### 13.3 Proyecto padre

```xml
<parent>
    <groupId>com.busconnect</groupId>
    <artifactId>busconnect-backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</parent>
```

**Herencia**: Hereda la version de Spring Boot, plugins de build y configuraciones comunes del POM padre.

### 13.4 Plugin de build

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

**Proposito**: Empaqueta la aplicacion como JAR ejecutable con todas las dependencias incluidas (fat JAR).

---

## 14. Apendice

### 14.1 Mapa de archivos relevantes

#### Codigo fuente

| Archivo | Lineas | Proposito | Estado |
|---------|--------|-----------|--------|
| [EurekaServiceApplication.java](src/main/java/com/busconnect/eurekaservice/EurekaServiceApplication.java) | ~14 | Bootstrap de Spring Boot + activacion Eureka Server | Activo |
| [EurekaService.java](src/main/java/com/busconnect/eurekaservice/service/EurekaService.java) | ~4 | Interfaz de servicio (placeholder) | Comentado |
| [EurekaServiceI.java](src/main/java/com/busconnect/eurekaservice/service/EurekaServiceI.java) | ~6 | Implementacion de servicio (placeholder) | Comentado |

#### Configuracion

| Archivo | Lineas | Proposito | Complejidad |
|---------|--------|-----------|-------------|
| [application.yml](src/main/resources/application.yml) | ~22 | Configuracion del servidor Eureka | Media |
| [pom.xml](pom.xml) | ~50 | Dependencias Maven | Baja |
| [Dockerfile](Dockerfile) | ~30 | Build multi-stage de imagen Docker | Media |
| [docker-compose.yml](docker-compose.yml) | ~8 | Servicio en Docker Compose | Trivial |

#### Documentacion

| Archivo | Proposito |
|---------|-----------|
| [README.md](README.md) | Documentacion funcional y guia de uso |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Diagramas de flujo y arquitectura |
| **TECHNICAL_DOCUMENTATION.md** | Este documento (analisis tecnico completo) |

### 14.2 Puertos y networking

| Componente | Puerto | Protocolo | Scope |
|------------|--------|-----------|-------|
| Eureka Server | 8761 | HTTP | Interno (microservicios) |
| API Gateway | 8080 | HTTP | Externo (publico) |
| User Service | 8082 | HTTP | Interno (via gateway) |
| Catalog Service | 8083 | HTTP | Interno (via gateway) |

**Networking en Docker**:
- Red: `busconnect-net` (custom bridge network)
- Eureka accesible via hostname `eureka-service` (o `eureka-server` segun compose)
- Puerto 8761 expuesto al host para acceso al dashboard en desarrollo

### 14.3 Variables de entorno criticas

| Variable | Valor por defecto | Valor Docker | Proposito |
|----------|-------------------|--------------|-----------|
| `SPRING_PROFILES_ACTIVE` | (ninguno) | `docker` | Activa perfil de configuracion |
| `JAVA_OPTS` | `-Xmx512m -Xms256m` | (mismo) | Configuracion JVM |

### 14.4 Comandos utiles

#### Build y ejecucion local
```bash
# Compilar desde la raiz del proyecto
mvn clean package -pl eureka-service -am -DskipTests

# Ejecutar
java -jar eureka-service/target/eureka-server-0.0.1-SNAPSHOT.jar

# Verificar que inicio correctamente
curl http://localhost:8761/actuator/health
```

#### Docker
```bash
# Build de imagen
docker build -t busconnect/eureka-service:latest .

# Ejecutar contenedor
docker run -p 8761:8761 busconnect/eureka-service:latest

# Ejecutar con Docker Compose
docker-compose up eureka-server

# Ver logs
docker logs -f eureka-server
```

#### Consultas al registro
```bash
# Ver todos los servicios registrados
curl http://localhost:8761/eureka/apps

# Ver servicios en formato JSON
curl -H "Accept: application/json" http://localhost:8761/eureka/apps | jq

# Ver un servicio especifico
curl http://localhost:8761/eureka/apps/USER-SERVICE

# Health check
curl http://localhost:8761/actuator/health | jq
```

### 14.5 Parametros de Eureka - Referencia rapida

#### Parametros del servidor (eureka.server.*)

| Parametro | Valor actual | Default | Descripcion |
|-----------|-------------|---------|-------------|
| `enable-self-preservation` | `false` | `true` | Modo de autopreservacion |
| `eviction-interval-timer-in-ms` | `5000` | `60000` | Intervalo de verificacion de leases |
| `renewal-percent-threshold` | (default) | `0.85` | Umbral para autopreservacion (85%) |
| `renewal-threshold-update-interval-ms` | (default) | `900000` | Intervalo de actualizacion del umbral (15min) |
| `response-cache-update-interval-ms` | (default) | `30000` | Cache de respuestas del servidor |

#### Parametros del cliente (eureka.client.* - configurados en microservicios)

| Parametro | Valor tipico | Descripcion |
|-----------|-------------|-------------|
| `register-with-eureka` | `true` | Registrarse en Eureka |
| `fetch-registry` | `true` | Descargar registro |
| `registry-fetch-interval-seconds` | `30` | Frecuencia de actualizacion del cache |
| `initial-instance-info-replication-interval-seconds` | `40` | Delay de registro inicial |
| `instance-info-replication-interval-seconds` | `30` | Frecuencia de replicacion |

#### Parametros de instancia (eureka.instance.* - configurados en microservicios)

| Parametro | Valor tipico | Descripcion |
|-----------|-------------|-------------|
| `lease-renewal-interval-in-seconds` | `30` | Frecuencia de heartbeat |
| `lease-expiration-duration-in-seconds` | `90` | Tiempo para expulsion sin heartbeat |
| `prefer-ip-address` | `true` | Registrar IP en lugar de hostname |
| `instance-id` | `${spring.application.name}:${random.value}` | ID unico de instancia |

### 14.6 Patrones de diseno identificados

#### Service Registry Pattern
**Descripcion**: Registro centralizado donde los servicios publican su ubicacion.
**Implementacion**: Toda la aplicacion (Eureka Server).

#### Client-Side Discovery Pattern
**Descripcion**: Los clientes consultan el registro y seleccionan una instancia.
**Implementacion**: Via Eureka Client + Ribbon en los microservicios consumidores.

#### Heartbeat Pattern
**Descripcion**: Verificacion periodica de disponibilidad mediante senales de vida.
**Implementacion**: PUT `/eureka/apps/{app}/{instance}` cada 30 segundos.

#### Self-Preservation Pattern
**Descripcion**: Proteccion contra expulsiones masivas durante particiones de red.
**Implementacion**: `eureka.server.enable-self-preservation` (actualmente deshabilitado).

#### Lease Pattern
**Descripcion**: Cada registro tiene un tiempo de vida limitado que debe renovarse.
**Implementacion**: Lease de 90 segundos, renovado cada 30 segundos via heartbeat.

### 14.7 Glosario tecnico

**Dashboard**: Interfaz web de Eureka que muestra el estado de los servicios registrados (accesible en `http://localhost:8761`).

**Eviction**: Proceso de eliminar instancias del registro que no han enviado heartbeat dentro del periodo de lease.

**Eureka**: Service Discovery de Netflix para registro y descubrimiento de microservicios.

**Fetch Registry**: Operacion donde un cliente descarga la lista completa de servicios registrados en Eureka.

**Heartbeat**: Senal periodica enviada por cada instancia para confirmar que sigue activa.

**Instance**: Una ejecucion particular de un microservicio, identificada por su `instanceId`.

**Lease**: Contrato temporal entre Eureka y una instancia que expira si no se renueva.

**Peer Awareness**: Capacidad de nodos Eureka de replicar registros entre si (modo cluster).

**Register**: Operacion donde un microservicio envia su metadata a Eureka al iniciar.

**Renewal**: Sinonimo de heartbeat; renovacion del lease de una instancia.

**Ribbon**: Load balancer client-side de Netflix, integrado con Eureka Client.

**Self-Preservation**: Modo de emergencia de Eureka que previene expulsiones masivas durante particiones de red.

**Service Discovery**: Mecanismo para localizar dinamicamente instancias de servicios sin conocer sus IPs.

**Sliding Window**: Ventana deslizante usada para calcular metricas (ej: tasa de renovaciones).

---

## Fin del Documento

**Fecha de generacion**: 2026-02-07
**Version analizada**: eureka-service 0.0.1-SNAPSHOT
**Commit base**: Verificar con `git log` en el repositorio
**Autor del analisis**: Claude Opus 4.6

**Proximos pasos sugeridos**:
1. Implementar cluster Eureka con 3+ nodos (alta prioridad para produccion)
2. Agregar autenticacion HTTP Basic al servidor
3. Configurar HTTPS/TLS
4. Eliminar clases comentadas (EurekaService.java, EurekaServiceI.java)
5. Corregir inconsistencia de nombres (eureka-service vs eureka-server)
6. Agregar tests de contexto de Spring (`@SpringBootTest`)
7. Habilitar autopreservacion para despliegue en produccion
8. Integrar con Spring Boot Admin para monitoreo centralizado
