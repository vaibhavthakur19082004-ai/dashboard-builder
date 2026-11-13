# Dashboard Builder – "Dashboard as a Service"

Full-stack project: users connect a dataset, define metrics and get live chart widgets (bar, line, KPI).
**Spring Boot 3 / Java 17** backend + **React (Vite) / Recharts** frontend.

## Features
| Requirement | Implementation |
|---|---|
| Dynamic aggregation | `MetricService` builds `GROUP BY` + `SUM/COUNT/AVG/MIN/MAX` with JPA `CriteriaBuilder` / `CriteriaQuery` from a JSON payload |
| SQL-injection safety | Field/aggregation/operator whitelists, typed + parameter-bound filter values, row limits |
| Caching | `@EnableCaching` + `@Cacheable` (Caffeine, 5 min TTL) in `CachedMetricService`; query record is the cache key |
| Security / RBAC | JWT auth filter, roles `ADMIN / EDITOR / VIEWER`, every query scoped to the caller's workspace (cross-tenant = 404) |
| Export | CSV (with formula-injection guard) and Excel via Apache POI |
| Error handling | `@RestControllerAdvice` returns a standard JSON error body |
| Docs | Swagger UI at `/swagger-ui.html` |
| Frontend | Login, dashboard list, drag chart types onto canvas, live-preview builder, drag to reorder, export buttons |

## Run it
```bash
# backend (needs Java 17 + Maven)
cd backend && mvn spring-boot:run          # http://localhost:8080  (H2 in-memory, 500 demo sales rows seeded)
# frontend (needs Node 18+)
cd frontend && npm install && npm run dev  # http://localhost:5173 (proxies /api to :8080)
```
Tests: `cd backend && mvn test`. Production: `--spring.profiles.active=prod` (PostgreSQL via `DB_URL/DB_USER/DB_PASSWORD`) and set `JWT_SECRET`.

## API quick reference
| Method | Path | Role |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login` | public |
| POST | `/api/users` | ADMIN |
| GET/POST/DELETE | `/api/dashboards[/{id}]` | read: any, write: ADMIN/EDITOR |
| POST | `/api/dashboards/{id}/widgets` | ADMIN/EDITOR |
| PUT | `/api/dashboards/{id}/widgets/order` | ADMIN/EDITOR |
| PUT/DELETE | `/api/widgets/{id}` | ADMIN/EDITOR |
| GET | `/api/widgets/{id}/data` (cached) | any |
| GET | `/api/dashboards/{id}/export?format=csv\|xlsx` | any |
| GET / POST | `/api/metrics/schema`, `/api/metrics/preview` | any |

Example widget payload:
```json
{ "title": "Revenue by region", "chartType": "BAR",
  "query": { "dimension": "region", "aggregation": "SUM", "measure": "amount",
             "filters": [ { "field": "channel", "op": "EQ", "value": "Online" } ] } }
```

## Structure
```
backend/src/main/java/com/axlero/dashboard/
  config/ controller/ dto/ entity/ exception/ repository/ security/ service/
frontend/src/ (App, api.js, components/)
```
