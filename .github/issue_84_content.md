## 📋 Summary

Added optional `userId` query parameter to POST `/api/routes/calculate` endpoint for user activity tracking in logs.

## 🎯 Motivation

Enable tracking of which users are searching for routes without adding database complexity. This provides valuable insights through logs for analytics and debugging purposes.

## 🔧 Changes Implemented

### Modified Files
- `catalog-service/src/main/java/com/busconnect/catalogservice/controller/RouteController.java`

### Technical Details

**Endpoint signature:**
```java
@PostMapping("/calculate")
public Mono<ResponseEntity<RouteResultResponse>> calculateRoute(
    @Valid @RequestBody CalculateRouteRequest request,
    @RequestParam(required = false) Long userId
)
```

**Logging behavior:**
- If `userId` is provided: `User {id} requested route: {origin} -> {destination}`
- If `userId` is null: `Route calculation requested: {origin} -> {destination}`

## 📝 Usage Examples

**With userId:**
```bash
curl -X POST "http://localhost:8083/api/routes/calculate?userId=2" \
  -H "Content-Type: application/json" \
  -d '{"originMunicipality":"Barcelona","destinationMunicipality":"Girona"}'
```

**Without userId (backwards compatible):**
```bash
curl -X POST "http://localhost:8083/api/routes/calculate" \
  -H "Content-Type: application/json" \
  -d '{"originMunicipality":"Barcelona","destinationMunicipality":"Girona"}'
```

## 🔍 Log Output Example

```
2026-01-18 13:28:18 - c.b.c.controller.RouteController - User 2 requested route: Barcelona -> Tarragona
```

## ✅ Benefits

- ✅ **No database changes required** - Pure logging solution
- ✅ **Backwards compatible** - Parameter is optional
- ✅ **Easy to implement** - Minimal code changes
- ✅ **Immediate insights** - Track user behavior in real-time
- ✅ **Frontend ready** - Can be integrated with user session context

## 🚀 Next Steps (Frontend)

This backend change enables the following frontend features:
1. User session context with localStorage
2. Search history tracking per user
3. Personalized dashboard with recent searches
4. User-specific analytics

## 🧪 Testing

Tested with:
- User ID 2 (Joan García): ✅ Logged correctly
- No user ID: ✅ Falls back to generic log
- API response: ✅ Unchanged, fully backwards compatible

## 📦 Deployment Status

- [x] Code changes implemented
- [x] Service rebuilt and redeployed
- [x] Manual testing completed
- [x] Logs verified

---

**Implementado por:** @Irina-Ichim  
**Fecha:** 2026-01-18  
**Servicio afectado:** catalog-service  
**Puerto:** 8083
