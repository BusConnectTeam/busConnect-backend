# Eureka Service - Arquitectura

## 📝 Service Registration Flow

```mermaid
sequenceDiagram
    participant Service as Microservice<br/>(catalog-service)
    participant Eureka as Eureka Server<br/>(8761)
    participant Registry as Service Registry
    
    Note over Service: Application Startup
    
    Service->>Service: Read Config<br/>eureka.client.service-url
    
    Service->>Eureka: POST /eureka/apps/CATALOG-SERVICE<br/>Register Instance
    
    Note over Service: Payload:<br/>- hostname: catalog-service<br/>- ipAddr: 172.18.0.3<br/>- port: 8083<br/>- status: STARTING<br/>- metadata: {...}
    
    Eureka->>Registry: Store Instance Info
    Registry-->>Eureka: Stored
    
    Eureka-->>Service: 204 No Content<br/>Registration Successful ✓
    
    Note over Service: Status: UP
    
    loop Every 30 seconds (Heartbeat)
        Service->>Eureka: PUT /eureka/apps/CATALOG-SERVICE/{instanceId}<br/>Heartbeat (renewLease)
        Eureka->>Registry: Update lastHeartbeat
        Eureka-->>Service: 200 OK
    end
    
    Note over Eureka: If no heartbeat for 90s<br/>→ Eviction
    
    Eureka->>Registry: Check lastHeartbeat
    
    alt Heartbeat OK
        Registry-->>Eureka: Instance Healthy ✓
    else No Heartbeat (>90s)
        Eureka->>Registry: Remove Instance
        Note over Registry: Service EVICTED ❌
    end
    
    Note over Service: Application Shutdown
    
    Service->>Eureka: DELETE /eureka/apps/CATALOG-SERVICE/{instanceId}<br/>Deregister
    Eureka->>Registry: Remove Instance
    Eureka-->>Service: 200 OK
    
    Note over Registry: Graceful Shutdown ✓
```

---

## 🔍 Service Discovery Flow

```mermaid
flowchart TB
    Start([API Gateway inicia]) --> Fetch1[Fetch Registry<br/>from Eureka]
    
    Fetch1 --> Cache[Local Registry Cache]
    
    Cache --> Ready[Gateway Ready]
    
    Ready --> Request[Client Request<br/>GET /api/users/123]
    
    Request --> Parse[Parse Route<br/>/api/users/** → USER-SERVICE]
    
    Parse --> LookupCache{Lookup in<br/>Local Cache}
    
    LookupCache -->|Cache Hit<br/>< 30s old| GetInstances[Get USER-SERVICE<br/>instances]
    
    LookupCache -->|Cache Miss<br/>or > 30s| FetchRegistry[Fetch from Eureka<br/>GET /eureka/apps]
    
    FetchRegistry --> UpdateCache[Update Local Cache]
    UpdateCache --> GetInstances
    
    GetInstances --> CheckInstances{Available<br/>Instances?}
    
    CheckInstances -->|No instances| ServiceDown[503 Service Unavailable<br/>No USER-SERVICE available]
    
    CheckInstances -->|1 instance| SingleInstance[user-service:8082<br/>172.18.0.4:8082]
    
    CheckInstances -->|Multiple instances| LoadBalance[Load Balancer<br/>Round Robin]
    
    LoadBalance --> Instance1[Instance 1<br/>172.18.0.4:8082]
    LoadBalance --> Instance2[Instance 2<br/>172.18.0.5:8082]
    LoadBalance --> Instance3[Instance 3<br/>172.18.0.6:8082]
    
    SingleInstance --> ForwardRequest[Forward Request<br/>to selected instance]
    Instance1 --> ForwardRequest
    Instance2 --> ForwardRequest
    Instance3 --> ForwardRequest
    
    ForwardRequest --> Response[Receive Response]
    Response --> ReturnClient[Return to Client]
    
    ServiceDown --> ReturnClient
    
    subgraph "Background Process (Every 30s)"
        AutoRefresh[Auto Refresh Registry] --> FetchUpdates[Fetch /eureka/apps/delta]
        FetchUpdates --> MergeCache[Merge with Local Cache]
    end
    
    style Cache fill:#4CAF50,stroke:#2E7D32,color:#fff
    style LoadBalance fill:#2196F3,stroke:#1565C0,color:#fff
    style ServiceDown fill:#f44336,stroke:#c62828,color:#fff
    style AutoRefresh fill:#FF9800,stroke:#E65100,color:#fff
```

---

## 💓 Health Check & Self-Preservation

```mermaid
stateDiagram-v2
    [*] --> Normal: Eureka Server Start
    
    Normal --> Monitoring: Monitor Heartbeats
    
    Monitoring --> CalculateRenewals: Calculate Renewal Rate<br/>Every 15 minutes
    
    CalculateRenewals --> CheckThreshold{Renewal Rate<br/>> 85%?}
    
    CheckThreshold -->|Yes ✓<br/>Healthy| Normal: Continue Normal Operation
    
    CheckThreshold -->|No ❌<br/>< 85%| SelfPreservation: ENTER SELF-PRESERVATION MODE
    
    Normal: 🟢 NORMAL MODE
    Normal: - Evict unhealthy instances
    Normal: - Remove after 90s no heartbeat
    Normal: - Registry up-to-date
    
    SelfPreservation: 🟡 SELF-PRESERVATION MODE
    SelfPreservation: - DO NOT evict instances
    SelfPreservation: - Keep all registered services
    SelfPreservation: - Assume network partition
    
    SelfPreservation --> WaitRecovery: Wait for Recovery
    
    WaitRecovery --> RecheckThreshold{Renewal Rate<br/>> 85%?}
    
    RecheckThreshold -->|No| SelfPreservation: Stay in Self-Preservation
    RecheckThreshold -->|Yes ✓| ExitSelfPreservation: EXIT SELF-PRESERVATION
    
    ExitSelfPreservation --> CleanupExpired[Cleanup Expired Instances]
    CleanupExpired --> Normal
    
    note right of SelfPreservation
        Warning en Dashboard:
        "EMERGENCY! EUREKA MAY BE 
        INCORRECTLY CLAIMING 
        INSTANCES ARE UP WHEN 
        IN FACT THEY ARE NOT."
    end note
    
    note right of CheckThreshold
        Threshold Calculation:
        Expected Renewals = 
        (Total Instances × 2) / minute
        
        Actual Renewals = 
        Count in last minute
        
        Rate = Actual / Expected
    end note
```

---

## 🔗 Referencias

- [README Principal](./README.md)
- [Configuración](./src/main/resources/application.yml)
- [Eureka Dashboard](http://localhost:8761)

