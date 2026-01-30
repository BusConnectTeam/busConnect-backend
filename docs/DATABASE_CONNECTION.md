# Conexion a la Base de Datos en Render

Guia para conectarte a la base de datos PostgreSQL de BusConnect desplegada en Render.

---

## Desde el Dashboard de Render

### Paso 1: Acceder a la base de datos

1. Ve a [render.com](https://render.com) e inicia sesion
2. En el Dashboard, busca **busconnect-db** en la lista de servicios
3. Click en el nombre de la base de datos

### Paso 2: Ver informacion de conexion

En la pagina de la base de datos veras varias secciones:

- **Status**: Debe mostrar `available` (disponible)
- **Connections**: Aqui estan las credenciales

### Paso 3: Tipos de conexion

| Tipo | Uso | Cuando usarlo |
|------|-----|---------------|
| **Internal Database URL** | Conexion entre servicios dentro de Render | Para tus microservicios (ya configurado en render.yaml) |
| **External Database URL** | Conexion desde fuera de Render | Para tu IDE, pgAdmin, o terminal local |
| **PSQL Command** | Comando listo para copiar/pegar | Para conectarte rapido desde terminal |

---

## Desde tu Terminal (IDE)

### Requisitos

- Tener `psql` instalado (viene con PostgreSQL)
- Verificar instalacion:
  ```bash
  psql --version
  ```

### Paso 1: Obtener la External Database URL

1. En Render Dashboard → **busconnect-db** → **Connections**
2. Busca **External Database URL**
3. Copia la URL completa (se ve asi):
   ```
   postgresql://TU_USUARIO:TU_PASSWORD@TU_HOST.frankfurt-postgres.render.com/TU_DATABASE
   ```

### Paso 2: Conectarte

Abre tu terminal y ejecuta:

```bash
psql "PEGA_AQUI_TU_EXTERNAL_DATABASE_URL"
```

**Ejemplo** (con URL ficticia):
```bash
psql "postgresql://myuser_abc:mypassword123@dpg-xxxxx.frankfurt-postgres.render.com/mydatabase"
```

**Importante:** Pon la URL entre comillas dobles `"`.

### Paso 3: Comandos utiles una vez conectado

```sql
-- Ver todos los schemas
\dn

-- Ver tablas en el schema user_service
\dt user_service.*

-- Ver tablas en el schema catalog
\dt catalog.*

-- Ver estructura de una tabla
\d user_service.users

-- Contar registros
SELECT COUNT(*) FROM user_service.users;
SELECT COUNT(*) FROM catalog.municipalities;

-- Ver algunos usuarios
SELECT id, email, first_name, role FROM user_service.users LIMIT 5;

-- Ver algunos municipios
SELECT id, name, province FROM catalog.municipalities LIMIT 10;

-- Salir de psql
\q
```

---

## Usando pgAdmin (Interfaz Grafica)

### Paso 1: Obtener credenciales individuales

En Render Dashboard → **busconnect-db** → **Connections**, encontraras:

| Campo | Donde encontrarlo |
|-------|-------------------|
| **Hostname** | En la seccion "Hostname" |
| **Port** | Generalmente `5432` |
| **Database** | En "Database" |
| **Username** | En "Username" |
| **Password** | Click en "Show" para revelar |

### Paso 2: Crear conexion en pgAdmin

1. Abre pgAdmin
2. Click derecho en **Servers** → **Register** → **Server**
3. Pestana **General**:
   - Name: `BusConnect Render` (o el nombre que quieras)
4. Pestana **Connection**:
   - Host: `(pega el hostname de Render)`
   - Port: `5432`
   - Maintenance database: `(pega el database name)`
   - Username: `(pega el username)`
   - Password: `(pega el password)`
5. Click **Save**

---

## Schemas de la Base de Datos

BusConnect usa schemas separados para cada microservicio:

```
busconnect_itsf (database)
├── catalog (schema)
│   ├── municipalities (tabla - 947 municipios de Catalunya)
│   ├── bus_companies (tabla - 8 empresas de autobuses)
│   ├── bus_types (tabla - 24 tipos de autobuses)
│   ├── drivers (tabla - 32 conductores)
│   └── flyway_schema_history (tabla)
├── user_service (schema)
│   ├── users (tabla - usuarios de la aplicacion)
│   └── flyway_schema_history (tabla)
└── public (schema por defecto)
```

### Ver todos los schemas

```sql
\dn
```

### Cambiar al schema de usuarios

```sql
SET search_path TO user_service;
\dt  -- ahora muestra tablas de user_service
```

---

## Troubleshooting

### Error: "connection refused"

**Causa:** La IP de tu red puede estar bloqueada.

**Solucion:**
1. En Render Dashboard → Base de datos → **Access Control**
2. Agrega tu IP publica o selecciona "Allow all IPs" (para desarrollo)

### Error: "password authentication failed"

**Causa:** Password incorrecto o copiado mal.

**Solucion:**
1. Ve a Render Dashboard → Base de datos → Connections
2. Click en "Show" para ver el password completo
3. Copia de nuevo asegurandote de no incluir espacios extra

### Error: "database does not exist"

**Causa:** Nombre de base de datos incorrecto.

**Solucion:**
1. Verifica el nombre exacto en Render Dashboard → Database name
2. Nota: Render agrega un sufijo aleatorio (ej: `busconnect_itsf`)

### Las tablas no existen

**Causa:** Flyway no ejecuto las migraciones.

**Solucion:** Ejecutar manualmente los scripts de migracion:
```bash
# Ver los archivos de migracion
ls user-service/src/main/resources/db/migration/
ls catalog-service/src/main/resources/db/migration/

# Ejecutar el SQL manualmente via psql
psql "TU_EXTERNAL_URL" -f user-service/src/main/resources/db/migration/V1__create_users_schema_and_seed_data.sql
```

---

## Ejecutar Migraciones Manualmente en Render

Cuando Flyway no ejecuta las migraciones automaticamente (por ejemplo, al crear una nueva base de datos o agregar nuevas tablas), puedes ejecutarlas manualmente.

### Paso 1: Ubicar los archivos de migracion

Los archivos SQL de migracion estan en:

```
catalog-service/src/main/resources/db/migration/
├── V1__create_schema_and_municipalities.sql  (schema catalog + municipios)
└── V2__create_companies_buses_drivers.sql    (empresas, buses, conductores)

user-service/src/main/resources/db/migration/
└── V1__create_users_schema_and_seed_data.sql (schema user_service + usuarios)
```

### Paso 2: Conectarte a la base de datos

```bash
psql "TU_EXTERNAL_DATABASE_URL"
```

### Paso 3: Ejecutar las migraciones

**Opcion A: Ejecutar archivo completo desde terminal (sin conectarte primero)**

```bash
# Migracion de catalog-service (municipios)
psql "TU_EXTERNAL_URL" -f catalog-service/src/main/resources/db/migration/V1__create_schema_and_municipalities.sql

# Migracion de catalog-service (empresas, buses, conductores)
psql "TU_EXTERNAL_URL" -f catalog-service/src/main/resources/db/migration/V2__create_companies_buses_drivers.sql

# Migracion de user-service
psql "TU_EXTERNAL_URL" -f user-service/src/main/resources/db/migration/V1__create_users_schema_and_seed_data.sql
```

**Opcion B: Copiar y pegar SQL estando conectado**

1. Conectate con `psql "TU_EXTERNAL_URL"`
2. Abre el archivo `.sql` en tu editor
3. Copia todo el contenido
4. Pega en la terminal de psql
5. Presiona Enter

### Paso 4: Verificar que se ejecuto correctamente

```sql
-- Ver todos los schemas creados
\dn

-- Verificar tablas en catalog
\dt catalog.*

-- Verificar tablas en user_service
\dt user_service.*

-- Contar registros para confirmar datos
SELECT COUNT(*) as empresas FROM catalog.bus_companies;
SELECT COUNT(*) as tipos_bus FROM catalog.bus_types;
SELECT COUNT(*) as conductores FROM catalog.drivers;
SELECT COUNT(*) as municipios FROM catalog.municipalities;
SELECT COUNT(*) as usuarios FROM user_service.users;
```

### Orden de ejecucion importante

Si ejecutas desde cero, sigue este orden:

1. `V1__create_schema_and_municipalities.sql` (catalog)
2. `V2__create_companies_buses_drivers.sql` (catalog)
3. `V1__create_users_schema_and_seed_data.sql` (user_service)

### Errores comunes al ejecutar migraciones

**Error: "relation already exists"**
- La tabla ya fue creada. Puedes ignorar si usas `IF NOT EXISTS`

**Error: "schema does not exist"**
- Ejecuta primero la migracion V1 que crea el schema

**Error: "duplicate key value violates unique constraint"**
- Los datos ya existen. Si usas `ON CONFLICT DO NOTHING`, se ignoran duplicados

**Error: "permission denied for schema"**
- El usuario no tiene permisos. En Render, el usuario generado tiene permisos completos

---

## Seguridad

- **Nunca** compartas tu External Database URL publicamente
- **Nunca** hagas commit de credenciales en el repositorio
- Usa variables de entorno para las credenciales en tu codigo
- Considera restringir las IPs permitidas en produccion

---

## Referencias

- [Render PostgreSQL Docs](https://render.com/docs/databases)
- [PostgreSQL psql Documentation](https://www.postgresql.org/docs/current/app-psql.html)
- [pgAdmin Documentation](https://www.pgadmin.org/docs/)
