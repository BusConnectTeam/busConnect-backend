# User Service - Arquitectura

## 👤 CRUD Operations Flow

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway
    participant User as User Service<br/>(8082)
    participant Cache as Caffeine Cache
    participant DB as PostgreSQL<br/>(user_service schema)
    
    Note over Client: CREATE USER
    Client->>Gateway: POST /api/users<br/>{name, email, role}
    Gateway->>User: POST /users
    
    User->>User: Validar datos<br/>email único, role válido
    User->>DB: INSERT INTO users<br/>isActive=true
    DB-->>User: User created (id=1)
    User->>Cache: Store user (1h TTL)
    User-->>Gateway: 201 Created
    Gateway-->>Client: {id: 1, name, email}
    
    Note over Client: READ USER
    Client->>Gateway: GET /api/users/1
    Gateway->>User: GET /users/1
    
    User->>Cache: ¿User en caché?
    Cache-->>User: Hit ✓
    User-->>Gateway: 200 OK
    Gateway-->>Client: {user data}
    
    Note over Client: UPDATE USER
    Client->>Gateway: PUT /api/users/1<br/>{name: "Updated"}
    Gateway->>User: PUT /users/1
    
    User->>DB: UPDATE users<br/>SET name=?, updatedAt=NOW()
    DB-->>User: Updated
    User->>Cache: Invalidate & Store
    User-->>Gateway: 200 OK
    Gateway-->>Client: {updated user}
    
    Note over Client: SOFT DELETE
    Client->>Gateway: DELETE /api/users/1
    Gateway->>User: DELETE /users/1
    
    User->>DB: UPDATE users<br/>SET isActive=false
    DB-->>User: Deleted (soft)
    User->>Cache: Invalidate
    User-->>Gateway: 204 No Content
    Gateway-->>Client: Success
```

---

## 🗃️ Soft Delete & Cache Strategy

```mermaid
flowchart TB
    Request[Request: GET /users/1] --> CacheCheck{¿En Caché?}
    
    CacheCheck -->|Hit ✓| CheckActive{isActive?}
    CacheCheck -->|Miss| QueryDB[Query PostgreSQL]
    
    QueryDB --> Found{Found?}
    Found -->|No| NotFound[404 Not Found]
    Found -->|Yes| CheckActive2{isActive?}
    
    CheckActive2 -->|true ✓| StoreCache[Store in Cache<br/>TTL: 1h<br/>Max: 500 users]
    CheckActive2 -->|false| NotFound
    
    StoreCache --> ReturnUser[200 OK + User Data]
    
    CheckActive -->|true ✓| ReturnUser
    CheckActive -->|false| Invalidate[Remove from Cache]
    Invalidate --> NotFound
    
    NotFound --> End[Client Response]
    ReturnUser --> End
    
    subgraph "Soft Delete Operation"
        DeleteReq[DELETE /users/1] --> UpdateDB[UPDATE users<br/>SET isActive=false]
        UpdateDB --> InvalidateCache[Invalidate Cache Key]
        InvalidateCache --> DeleteResp[204 No Content]
    end
    
    subgraph "Restore Operation"
        RestoreReq[PUT /users/1/restore] --> UpdateDB2[UPDATE users<br/>SET isActive=true]
        UpdateDB2 --> InvalidateCache2[Invalidate Cache Key]
        InvalidateCache2 --> RestoreResp[200 OK]
    end
    
    style CacheCheck fill:#4CAF50,stroke:#2E7D32,color:#fff
    style NotFound fill:#f44336,stroke:#c62828,color:#fff
    style StoreCache fill:#2196F3,stroke:#1565C0,color:#fff
    style DeleteReq fill:#FF9800,stroke:#E65100,color:#fff
    style RestoreReq fill:#9C27B0,stroke:#6A1B9A,color:#fff
```

### Cache Configuration

| Parámetro | Valor | Descripción |
|-----------|-------|-------------|
| Max Entries | 500 users | Límite de usuarios en caché |
| TTL | 1 hora | Tiempo de vida |
| Eviction Policy | LRU | Least Recently Used |
| Hit Rate Target | >80% | Objetivo de eficiencia |

---

## 📊 Domain Model

```mermaid
classDiagram
    class User {
        -Long id
        -String name
        -String email
        -String password
        -Role role
        -Boolean isActive
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
        +create() User
        +update() User
        +softDelete() void
        +restore() void
        +isActive() boolean
    }
    
    class Role {
        <<enumeration>>
        ADMIN
        USER
        DRIVER
        COMPANY
    }
    
    class UserController {
        +createUser(UserRequestDTO) Mono~UserResponseDTO~
        +getUserById(Long) Mono~UserResponseDTO~
        +updateUser(Long, UserRequestDTO) Mono~UserResponseDTO~
        +deleteUser(Long) Mono~Void~
        +restoreUser(Long) Mono~UserResponseDTO~
        +searchByName(String) Flux~UserResponseDTO~
        +searchByRole(Role) Flux~UserResponseDTO~
    }
    
    class UserService {
        +create(UserRequestDTO) Mono~User~
        +findById(Long) Mono~User~
        +update(Long, UserRequestDTO) Mono~User~
        +softDelete(Long) Mono~Void~
        +restore(Long) Mono~User~
        +searchByName(String) Flux~User~
        +searchByRole(Role) Flux~User~
    }
    
    class UserRepository {
        <<interface>>
        +findById(Long) Mono~User~
        +findByEmail(String) Mono~User~
        +findByIsActiveTrue() Flux~User~
        +findByNameContainingAndIsActiveTrue(String) Flux~User~
        +findByRoleAndIsActiveTrue(Role) Flux~User~
        +save(User) Mono~User~
    }
    
    class CacheManager {
        +get(String key) Optional~User~
        +put(String key, User value) void
        +invalidate(String key) void
        +clear() void
    }
    
    User --> Role : has
    UserController --> UserService : uses
    UserService --> UserRepository : uses
    UserService --> CacheManager : uses
    UserRepository --> User : manages
```

### Database Schema

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_active ON users(is_active);
CREATE INDEX idx_users_name ON users(name);
```

---

## 🎯 Endpoints Summary

| Método | Endpoint | Descripción | Cache |
|--------|----------|-------------|-------|
| POST | `/users` | Crear usuario | Store on create |
| GET | `/users/{id}` | Obtener por ID | Read-through |
| PUT | `/users/{id}` | Actualizar usuario | Write-through |
| DELETE | `/users/{id}` | Soft delete | Invalidate |
| PUT | `/users/{id}/restore` | Restaurar usuario | Invalidate |
| GET | `/users/search/name?q={name}` | Buscar por nombre | No cache |
| GET | `/users/search/role?role={role}` | Buscar por rol | No cache |

---

## 🔐 Role-Based Access Control

```mermaid
graph LR
    Admin[ADMIN] -->|Full Access| AllOps[Create, Read,<br/>Update, Delete,<br/>Restore]
    
    Company[COMPANY] -->|Manage Drivers| DriverOps[Read Drivers,<br/>Update Drivers]
    
    Driver[DRIVER] -->|Self Only| SelfOps[Read Self,<br/>Update Self]
    
    User[USER] -->|Read Only| ReadOps[Read Self]
    
    style Admin fill:#f44336,stroke:#c62828,color:#fff
    style Company fill:#FF9800,stroke:#E65100,color:#fff
    style Driver fill:#2196F3,stroke:#1565C0,color:#fff
    style User fill:#4CAF50,stroke:#2E7D32,color:#fff
```

---

## 🔗 Referencias

- [README Principal](./README.md)
- [Configuración](./src/main/resources/application.yml)
- [User Model](./src/main/java/com/busconnect/userservice/model/User.java)
