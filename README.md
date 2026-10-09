# Product CRUD API

A REST API for managing products and items, built with Java 21 and Spring Boot 3.3.4. The application includes JWT authentication with refresh-token rotation, role-based access control, PostgreSQL persistence, Redis-backed refresh tokens, Flyway migrations, and asynchronous audit logging. This version is prepared for a later Prometheus/Grafana monitoring exercise; it intentionally does **not** include Prometheus or Grafana services/configuration.

## Technology stack

- Java 21, Spring Boot 3.3.4, Gradle
- Spring Web, Spring Data JPA, Spring Security, Bean Validation
- PostgreSQL 16, Redis 7, Flyway
- JWT access/refresh tokens
- Spring Boot Actuator and Micrometer Prometheus registry (application-side instrumentation only)
- Docker and Docker Compose

## Application architecture

```text
Client -> Product API (Spring Boot :8080) -> PostgreSQL
                                      -> Redis (refresh-token store)
                                      -> Actuator health/metrics endpoints
```

PostgreSQL and Redis are accessible only on the internal Docker network in the provided Compose configuration. The application port is bound to `127.0.0.1` on the host so it is not exposed directly to the public internet by default. On EC2, access it through an SSH tunnel unless you intentionally design a secured public ingress.

## Prepare configuration

1. Install Docker Engine/Desktop with Docker Compose support.
2. Copy the environment template:

   ```bash
   cp .env.example .env
   ```

   On Windows Git Bash, the same command works from the project root.
3. Edit `.env` and replace every `REPLACE_...` placeholder with strong unique values. Use a randomly generated JWT secret of at least 32 characters. Do not commit `.env` or paste its secrets into tickets/chat.

Compose intentionally fails fast if required secret variables are missing.

## Build and run the application

```bash
docker compose config
docker compose up --build -d
docker compose ps
```

Check logs if the application does not start:

```bash
docker compose logs --tail=100 app
docker compose logs --tail=100 postgres
docker compose logs --tail=100 redis
```

The API is available locally at `http://localhost:8080`. Swagger UI is at `http://localhost:8080/swagger-ui.html`.

## Health and metrics endpoints

Actuator is configured to expose only these endpoints:

- `GET /actuator/health` — application health status, without component details.
- `GET /actuator/prometheus` — application metrics in Prometheus exposition format.

Test them after the app starts:

```bash
curl -i http://localhost:8080/actuator/health
curl -i http://localhost:8080/actuator/prometheus
```

The health endpoint should return HTTP 200 when the application is healthy. The Prometheus endpoint should return HTTP 200 and text metrics. These are expected checks; record the actual output when you run them.

The endpoints are permitted by application security because a future Prometheus container will need to scrape them. In this Compose setup the app's host port is bound to localhost, while containers on the Docker network can reach the app internally. Do not publish the metrics endpoint to the public internet without an appropriate access-control design.

## API endpoints

| Method | Path | Access |
|---|---|---|
| POST | `/api/v1/auth/login` | Public |
| POST | `/api/v1/auth/refresh` | Public |
| POST | `/api/v1/auth/logout` | Public |
| GET | `/api/v1/products` | USER, ADMIN |
| GET | `/api/v1/products/{id}` | USER, ADMIN |
| POST | `/api/v1/products` | ADMIN |
| PUT | `/api/v1/products/{id}` | ADMIN |
| DELETE | `/api/v1/products/{id}` | ADMIN |
| GET | `/api/v1/products/{id}/items` | USER, ADMIN |
| POST | `/api/v1/products/{id}/items` | ADMIN |

Product listing supports pagination, for example `?page=0&size=20&sort=productName,asc`.

## Run tests

If you have Gradle installed locally:

```bash
gradle test
```

The Dockerfile builds the app with Gradle in a build stage, so a local Gradle installation is not required for the containerized run.

## Stop the stack

```bash
docker compose down
```

To also delete the PostgreSQL data volume (destructive):

```bash
docker compose down -v
```

## Monitoring exercise boundary

This project includes only the Spring Boot Actuator and Micrometer dependencies/configuration needed to expose application metrics. It intentionally does **not** include a Prometheus configuration, Prometheus service, Grafana service, Grafana datasource, or dashboard. Those will be created and configured hands-on as part of the monitoring phase.

## Security notes

- Keep `.env` private; it is ignored by Git. `.env.example` contains placeholders only.
- PostgreSQL and Redis have no host-published ports in the provided Compose file.
- The app is bound to host loopback by default. On EC2, access through an SSH tunnel while learning, and keep security-group inbound rules minimal.
- The seeded admin/user passwords must be replaced through `.env`. CORS remains permissive in the original application and should be restricted before any production exposure.
- This is a learning deployment, not a production-hardened reference architecture.
