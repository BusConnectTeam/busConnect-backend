# Despliegue en Render - BusConnect Backend

Este documento describe las decisiones técnicas y configuraciones realizadas para desplegar la arquitectura de microservicios de BusConnect en Render utilizando el plan gratuito.

## Índice

1. [Arquitectura Desplegada](#arquitectura-desplegada)
2. [Decisiones Técnicas](#decisiones-técnicas)
3. [Configuración del Blueprint](#configuración-del-blueprint)
4. [Variables de Entorno](#variables-de-entorno)
5. [Limitaciones del Plan Gratuito](#limitaciones-del-plan-gratuito)
6. [Consideraciones de Producción](#consideraciones-de-producción)

---

## Arquitectura Desplegada

```
┌─────────────────────────────────────────────────────────────────┐
│                         RENDER CLOUD                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────┐      ┌─────────────────────┐          │
│  │   busconnect-db     │      │  busconnect-eureka  │          │
│  │   (PostgreSQL 18)   │      │  (Service Discovery)│          │
│  │   Plan: Free        │      │  Type: Web Service  │          │
│  └──────────┬──────────┘      └──────────┬──────────┘          │
│             │                            │                      │
│             │                            │                      │
│  ┌──────────▼──────────┐      ┌──────────▼──────────┐          │
│  │ busconnect-user-    │      │ busconnect-catalog- │          │
│  │ service             │      │ service             │          │
│  │ Type: Web Service   │      │ Type: Web Service   │          │
│  └──────────┬──────────┘      └──────────┬──────────┘          │
│             │                            │                      │
│             └──────────┬─────────────────┘                      │
│                        │                                        │
│             ┌──────────▼──────────┐                            │
│             │ busconnect-api-     │                            │
│             │ gateway             │◄─── Punto de entrada       │
│             │ Type: Web Service   │     público                │
│             └─────────────────────┘                            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Servicios Desplegados

| Servicio | Tipo | Puerto | URL Pública |
|----------|------|--------|-------------|
| busconnect-db | PostgreSQL Managed | 5432 | Internal connection string |
| busconnect-eureka | Web Service | 8761 | https://busconnect-eureka.onrender.com |
| busconnect-user-service | Web Service | 8082 | https://busconnect-user-service.onrender.com |
| busconnect-catalog-service | Web Service | 8083 | https://busconnect-catalog-service.onrender.com |
| busconnect-api-gateway | Web Service | 8080 | https://busconnect-api-gateway.onrender.com |

---

## Decisiones Técnicas

### 1. Base de Datos: PostgreSQL Gestionado vs Docker Container

**Problema inicial:**
```yaml
# ❌ Configuración original - NO FUNCIONA en plan free
- type: pserv
  name: busconnect-postgres
  env: docker
  disk:
    name: postgres-data
    mountPath: /var/lib/postgresql/data
    sizeGB: 1
```

**Error:** `disks are not supported for free tier services`

**Solución adoptada:**
```yaml
# ✅ Usar base de datos gestionada de Render
databases:
  - name: busconnect-db
    plan: free
    region: frankfurt
    databaseName: busconnect
```

**Justificación:**
- Los discos persistentes (`disk`) requieren plan de pago
- La base de datos gestionada de Render incluye persistencia en el plan gratuito
- Beneficios adicionales: backups automáticos, mantenimiento gestionado, SSL incluido
- Se eliminó el archivo `Dockerfile.postgres` ya que no es necesario

### 2. Tipo de Servicio: Web Services vs Private Services

**Problema inicial:**
```yaml
# ❌ Private services - NO DISPONIBLE en plan free
- type: pserv
  name: busconnect-eureka
```

**Error:** `service type is not available for this plan`

**Solución adoptada:**
```yaml
# ✅ Usar web services
- type: web
  name: busconnect-eureka
```

**Justificación:**
- Los Private Services (`pserv`) solo están disponibles en planes de pago
- Los Web Services (`web`) son gratuitos y funcionan para nuestro caso de uso
- **Trade-off:** Los servicios son públicamente accesibles (no es ideal para producción real)

### 3. Comunicación entre Servicios: URLs Públicas

**Problema:**
Con Private Services, los servicios se comunican internamente usando nombres de host (`http://busconnect-eureka:8761`). Con Web Services, esto no funciona.

**Solución adoptada:**
```yaml
# ✅ Usar URLs públicas de Render
- key: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
  value: https://busconnect-eureka.onrender.com/eureka/
```

**Justificación:**
- Los Web Services en Render se comunican a través de sus URLs públicas
- Render proporciona HTTPS automático para todos los servicios
- Los servicios se registran en Eureka usando estas URLs públicas

### 4. Variables de Entorno para Base de Datos

**Configuración original en Spring:**
```yaml
r2dbc:
  url: r2dbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:busconnectdb}
  username: ${DB_USERNAME:busconnect_user}
  password: ${DB_PASSWORD}
```

**Solución en render.yaml:**
```yaml
envVars:
  - key: DB_HOST
    fromDatabase:
      name: busconnect-db
      property: host
  - key: DB_PORT
    fromDatabase:
      name: busconnect-db
      property: port
  - key: DB_NAME
    fromDatabase:
      name: busconnect-db
      property: database
  - key: DB_USERNAME
    fromDatabase:
      name: busconnect-db
      property: user
  - key: DB_PASSWORD
    fromDatabase:
      name: busconnect-db
      property: password
```

**Justificación:**
- Render inyecta automáticamente las credenciales de la base de datos
- No es necesario hardcodear valores sensibles
- Las credenciales se mantienen sincronizadas si Render las regenera

### 5. Usuario de Base de Datos

**Problema:**
```yaml
# ❌ No se puede especificar usuario personalizado
databases:
  - name: busconnect-db
    user: postgres  # Error: not a valid DB user name
```

**Solución:**
```yaml
# ✅ Dejar que Render genere el usuario
databases:
  - name: busconnect-db
    plan: free
    region: frankfurt
    databaseName: busconnect
    # Sin especificar user - Render lo genera automáticamente
```

**Justificación:**
- Render genera automáticamente un usuario seguro
- El nombre de usuario se obtiene via `fromDatabase.property: user`

---

## Configuración del Blueprint

### Archivo render.yaml Final

```yaml
# Base de datos PostgreSQL gestionada por Render
databases:
  - name: busconnect-db
    plan: free
    region: frankfurt
    databaseName: busconnect

services:
  # Eureka Service Discovery
  - type: web
    name: busconnect-eureka
    env: docker
    plan: free
    region: frankfurt
    dockerfilePath: ./eureka-service/Dockerfile
    dockerContext: .
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: prod
      - key: SERVER_PORT
        value: 8761
    healthCheckPath: /actuator/health

  # User Service
  - type: web
    name: busconnect-user-service
    env: docker
    plan: free
    region: frankfurt
    dockerfilePath: ./user-service/Dockerfile
    dockerContext: .
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: prod
      - key: SERVER_PORT
        value: 8082
      - key: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
        value: https://busconnect-eureka.onrender.com/eureka/
      - key: DB_HOST
        fromDatabase:
          name: busconnect-db
          property: host
      - key: DB_PORT
        fromDatabase:
          name: busconnect-db
          property: port
      - key: DB_NAME
        fromDatabase:
          name: busconnect-db
          property: database
      - key: DB_USERNAME
        fromDatabase:
          name: busconnect-db
          property: user
      - key: DB_PASSWORD
        fromDatabase:
          name: busconnect-db
          property: password
    healthCheckPath: /actuator/health

  # Catalog Service
  - type: web
    name: busconnect-catalog-service
    env: docker
    plan: free
    region: frankfurt
    dockerfilePath: ./catalog-service/Dockerfile
    dockerContext: .
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: prod
      - key: SERVER_PORT
        value: 8083
      - key: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
        value: https://busconnect-eureka.onrender.com/eureka/
      - key: DB_HOST
        fromDatabase:
          name: busconnect-db
          property: host
      - key: DB_PORT
        fromDatabase:
          name: busconnect-db
          property: port
      - key: DB_NAME
        fromDatabase:
          name: busconnect-db
          property: database
      - key: DB_USERNAME
        fromDatabase:
          name: busconnect-db
          property: user
      - key: DB_PASSWORD
        fromDatabase:
          name: busconnect-db
          property: password
      - key: OPENROUTESERVICE_API_KEY
        sync: false
    healthCheckPath: /actuator/health

  # API Gateway (Punto de entrada público)
  - type: web
    name: busconnect-api-gateway
    env: docker
    plan: free
    region: frankfurt
    dockerfilePath: ./api-gateway/Dockerfile
    dockerContext: .
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: prod
      - key: SERVER_PORT
        value: 8080
      - key: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
        value: https://busconnect-eureka.onrender.com/eureka/
    healthCheckPath: /actuator/health
```

---

## Variables de Entorno

### Variables Automáticas (inyectadas por Render)

| Variable | Origen | Descripción |
|----------|--------|-------------|
| `DB_HOST` | `fromDatabase.host` | Host de PostgreSQL |
| `DB_PORT` | `fromDatabase.port` | Puerto (5432) |
| `DB_NAME` | `fromDatabase.database` | Nombre de la base de datos |
| `DB_USERNAME` | `fromDatabase.user` | Usuario generado por Render |
| `DB_PASSWORD` | `fromDatabase.password` | Contraseña generada por Render |

### Variables Manuales (requieren configuración)

| Variable | Servicio | Descripción |
|----------|----------|-------------|
| `OPENROUTESERVICE_API_KEY` | catalog-service | API key de OpenRouteService (obtener en openrouteservice.org) |

**Nota:** La opción `sync: false` en `OPENROUTESERVICE_API_KEY` indica que esta variable debe configurarse manualmente en el dashboard de Render.

---

## Limitaciones del Plan Gratuito

### Limitaciones Actuales

| Limitación | Impacto | Mitigación |
|------------|---------|------------|
| **Sin Private Services** | Servicios expuestos públicamente | Aceptable para desarrollo/demo |
| **Sin discos persistentes** | No se puede usar PostgreSQL en Docker | Usar base de datos gestionada |
| **Cold starts** | Servicios se "duermen" tras 15 min de inactividad | Primera request puede tardar ~30s |
| **512 MB RAM** | Límite de memoria por servicio | Optimizar configuración de JVM |
| **Base de datos expira** | BD gratuita expira tras 90 días | Renovar o migrar a plan pago |

### Recomendaciones para Cold Starts

Los servicios gratuitos de Render entran en "sleep mode" después de 15 minutos de inactividad. Para mitigar:

1. **Usar un servicio de ping externo** (ej: UptimeRobot) para mantener los servicios activos
2. **Aceptar el cold start** para entornos de desarrollo/demo
3. **Migrar a plan de pago** para producción real

---

## Consideraciones de Producción

Si se planea usar esta configuración en producción real, considerar:

### 1. Seguridad
- **Migrar a Private Services** (plan de pago) para que los microservicios no sean públicos
- Implementar **autenticación entre servicios** (JWT, mTLS)
- Configurar **firewalls y rate limiting**

### 2. Rendimiento
- **Upgrade de plan** para evitar cold starts
- **Aumentar recursos** (RAM, CPU) según necesidades
- Considerar **múltiples instancias** para alta disponibilidad

### 3. Base de Datos
- **Migrar a plan de pago** antes de los 90 días de expiración
- Configurar **backups automáticos**
- Considerar **read replicas** para escalabilidad

### 4. Monitorización
- Configurar **alertas** en Render
- Integrar con herramientas de **APM** (Application Performance Monitoring)
- Revisar **logs** regularmente

---

## Comandos Útiles

### Desplegar cambios
```bash
git add .
git commit -m "descripción del cambio"
git push origin develop
```
Render detectará automáticamente los cambios y redesplegará.

### Ver logs de un servicio
En el dashboard de Render: Servicio → Logs

### Reiniciar un servicio
En el dashboard de Render: Servicio → Manual Deploy → Deploy latest commit

---

## Referencias

- [Render Blueprint Specification](https://render.com/docs/blueprint-spec)
- [Render PostgreSQL](https://render.com/docs/databases)
- [Render Environment Variables](https://render.com/docs/environment-variables)
- [Spring Boot on Render](https://render.com/docs/deploy-spring-boot)
