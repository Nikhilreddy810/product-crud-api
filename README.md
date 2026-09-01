# Product API

RESTful CRUD API for Products/Items — Spring Boot 3, Java 17, PostgreSQL, Redis, JWT auth with refresh token rotation.

## Stack

- Java 21, Spring Boot 3.3.4
- Spring Data JPA (Hibernate) + PostgreSQL
- Spring Security + JWT (jjwt) — access token (15 min) + refresh token (7 days, rotated, stored in Redis)
- Redis — refresh token store
- Flyway — DB schema migrations (`src/main/resources/db/migration`), `ddl-auto: validate` (Hibernate never mutates schema)
- springdoc-openapi (Swagger UI)
- JUnit 5 + Mockito + Spring Boot Test + H2 (tests)
- Gradle build
- Docker / Docker Compose

## Architecture

```
controller/   -> REST endpoints, request/response mapping
service/      -> interfaces + impl, business logic
repository/   -> Spring Data JPA repositories
entity/       -> JPA entities (Product, Item, AppUser)
dto/          -> request/response payloads (never expose entities directly)
security/     -> JwtUtil, JwtAuthFilter, RefreshTokenService (Redis), CustomUserDetailsService
config/       -> SecurityConfig, OpenApiConfig, AsyncConfig, DataSeeder
exception/    -> custom exceptions + GlobalExceptionHandler (@RestControllerAdvice)
```

Layering: Controller -> Service (interface) -> ServiceImpl -> Repository. Controllers never touch repositories directly.

### Auth flow

1. `POST /api/v1/auth/login` with username/password -> access token + refresh token.
2. Access token sent as `Authorization: Bearer <token>` on every request; `JwtAuthFilter` validates it and sets the security context.
3. `POST /api/v1/auth/refresh` with a refresh token -> old refresh token is revoked in Redis, a new access+refresh pair is issued (rotation).
4. `POST /api/v1/auth/logout` revokes the refresh token in Redis.

Roles: `ROLE_ADMIN` can create/update/delete products. `ROLE_ADMIN` and `ROLE_USER` can read.

Seeded users (created on first boot via `DataSeeder`, override via env vars):
| username | password | role |
|---|---|---|
| admin | admin123 | ROLE_ADMIN |
| user  | user123  | ROLE_USER  |

## Endpoints

| Method | Path | Role |
|---|---|---|
| POST | /api/v1/auth/login | public |
| POST | /api/v1/auth/refresh | public |
| POST | /api/v1/auth/logout | public |
| GET | /api/v1/products | USER, ADMIN |
| GET | /api/v1/products/{id} | USER, ADMIN |
| POST | /api/v1/products | ADMIN |
| PUT | /api/v1/products/{id} | ADMIN |
| DELETE | /api/v1/products/{id} | ADMIN |
| GET | /api/v1/products/{id}/items | USER, ADMIN |
| POST | /api/v1/products/{id}/items | ADMIN |

`GET /api/v1/products` and `GET /api/v1/products/{id}/items` support `?page=0&size=20&sort=productName,asc`.

## Run locally with Docker

```bash
docker compose up --build
```

App: http://localhost:8080
Swagger UI: http://localhost:8080/swagger-ui.html

## Run without Docker

Requires local Postgres + Redis running, then:

```bash
export DB_HOST=localhost DB_PORT=5432 DB_NAME=productdb DB_USER=postgres DB_PASSWORD=postgres
export REDIS_HOST=localhost REDIS_PORT=6379
./gradlew bootRun
```

If `gradlew` has no wrapper jar in your checkout, run `gradle wrapper` once (any local Gradle 8.x install) to generate it, or just use your local `gradle bootRun`.

## Async processing

Every product create/update/delete fires an audit log write (`audit_log` table — who did what, when) on a separate thread pool (`AsyncConfig.taskExecutor`, via `@Async("taskExecutor")` in `AuditLogServiceImpl`) so it never adds latency to the API response. Failures in the audit write are caught and logged, never propagated to the caller.

## Tests

```bash
./gradlew test
```

- **Unit tests (Mockito)**: `ProductServiceImpl`, `AuthServiceImpl` (login, refresh rotation, expired/invalid token handling, logout), `ItemServiceImpl`.
- **Integration tests** (`@SpringBootTest` + MockMvc + H2, `test` profile): `ProductController` — create/read, RBAC (403 for non-admin write), 401 unauthenticated, 404 not found, 400 validation.

Not covered: `AuthController` end-to-end (would require a running Redis in the test profile, since refresh-token storage is real Redis, not mocked, in the current test setup) — the underlying `AuthServiceImpl` logic is fully unit-tested instead.

## Sample requests

```bash
# login
curl -X POST localhost:8080/api/v1/auth/login -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# create product
curl -X POST localhost:8080/api/v1/products -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" -d '{"productName":"Laptop"}'

# refresh
curl -X POST localhost:8080/api/v1/auth/refresh -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'
```

## Notes / trade-offs

- Schema is managed entirely by Flyway (`V1__init_schema.sql`); Hibernate is `ddl-auto: validate` and will fail fast on startup if entities drift from the migration. Test profile disables Flyway and uses `create-drop` against H2 instead.
- Refresh tokens are JWTs with a `jti`, stored in Redis keyed by `jti -> username` with TTL = token expiry; rotation deletes the old key and stores a new one on every refresh call.
- CORS is wide-open (`*`) for dev; restrict `allowedOriginPatterns` before shipping.
- HTTPS enforcement is expected at the reverse-proxy/ingress layer, not in-app.
