# PostgreSQL and Flyway

The API applies migrations from `src/main/resources/db/migration` at startup. PostgreSQL stores inventory, readings, alerts, simulations, and command history.

Create a new migration with the next available number for schema changes. Preserve migrations already applied and back up any data you need to keep.

Use separate databases and credentials for testing and production. Store backups outside Git and verify that they can be restored. To validate a clean schema, run `mvn -Dtest=PostgresSchemaIntegrationTest test` with Docker running.
