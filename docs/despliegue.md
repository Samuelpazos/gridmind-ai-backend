# Deploy the API

- Run the Spring Boot API on Java 25 with a private PostgreSQL instance.
- Set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` in the hosting provider.
- Set `SPRING_PROFILES_ACTIVE=production`. The public proxy must terminate TLS and overwrite `X-Forwarded-Proto` with the original scheme.
- Set `ADMIN_EMAIL` and `ADMIN_PASSWORD` for the account that keeps the existing inventory and receives ESP32 readings. Keep `DEMO_MODE=false`. Enable `AUTH_REGISTRATION_ENABLED=true` if visitors should be able to register and create their own inventory.
- Store `TELEMETRY_DEVICE_TOKEN` and external service keys in the provider's secret settings.

Before sharing the URL, check `GET /health`, sign in over HTTPS, and verify the session cookie's Secure and HttpOnly attributes. Use two accounts to verify that each can access only its own inventory.

Flyway uses the PostgreSQL role `gridmind_workspace` to enforce row-level access. If the migration account cannot create roles, ask the database administrator to create it with NOLOGIN, NOINHERIT, NOBYPASSRLS, and no administrative privileges, and grant the Flyway account permission to administer that role. The API's database account needs permission to run `SET ROLE gridmind_workspace`.

At startup, existing unowned substations are assigned to `ADMIN_EMAIL`. Keep that address while using the current ESP32 token.

Use the [production environment example](../.env.production.example) as a template and fill in the private values in the provider.
