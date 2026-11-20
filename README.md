# GridMind AI · Backend

Java 25 and Spring Boot provide the REST API, JPA persistence, server sessions, CSRF protection, and workspace isolation. PostgreSQL 16 stores operational data. Smile runs Random Forest classification and Isolation Forest anomaly detection. The implementation combines Observer, Strategy, State, Abstract Factory, Command, Memento, Composite, Facade, Builder, and Chain of Responsibility.

## Local development

```bash
mvn clean package
mvn spring-boot:run
```

Outside Docker, set `SPRING_DATASOURCE_PASSWORD` and, as needed, `SPRING_DATASOURCE_URL` and `SPRING_DATASOURCE_USERNAME`. Compose reads bootstrap variables from the root `.env`. Use `ADMIN_EMAIL` and `ADMIN_PASSWORD` for the bootstrap account that owns existing inventory and receives legacy ESP32 telemetry. The optional demo uses `DEMO_EMAIL` and `DEMO_PASSWORD`; set `DEMO_MODE=false` to disable it. Set `AUTH_REGISTRATION_ENABLED=false` to close public registration. Bootstrap passwords must be 12–72 characters. Passwords are stored with BCrypt. The default sign-in rate limit is five failed attempts per IP in 15 minutes, and sessions expire after eight hours.

Swagger UI is available at `http://localhost:8080/swagger-ui.html`. Run `mvn test` for unit tests and start Docker to include PostgreSQL integration tests.

## Claude

Set `CLAUDE_API_KEY` in the backend environment for free-form alert questions and on-demand predictions. The key stays on the server. The in-memory limit, `CLAUDE_MAX_REQUESTS_PER_DAY`, defaults to 20 requests per process and resets at the UTC day boundary or server restart. Automatic monitoring uses Smile, and quick alert questions use stored GridMind data.

Use **Predecir con Claude** or `POST /api/components/{id}/claude-prediction` for an advisory estimate based on recent sensor readings. The legacy Gemini endpoint remains a temporary alias.

## Persistence

Flyway loads `src/main/resources/db/migration`. Keep the historical copies under `database/migrations` separate. The daily retention job removes expired readings, predictions, and anomalies after `GRIDMIND_RETENTION_DAYS` (seven days by default). Alerts and audit records are retained by this job.

## Deployment

Use HTTPS and set `SPRING_PROFILES_ACTIVE=production`. A separately hosted frontend uses `VITE_API_BASE_URL` ending in `/api`. Configure its exact origin in `CORS_ALLOWED_ORIGIN_PATTERNS` and test session cookies in the deployed browser.

- [Deploy the API](docs/despliegue.md)
- [PostgreSQL and Flyway](docs/postgresql.md)
- [Access and permissions](docs/permisos.md)
- [Hardware telemetry contract](docs/telemetria.md)
- [Backend verification](docs/verificacion.md)
- [Release verification checklist](docs/release-checklist.md)
