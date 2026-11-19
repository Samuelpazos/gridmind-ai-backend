# PostgreSQL y Flyway

La API aplica las migraciones de `src/main/resources/db/migration` al iniciar. PostgreSQL guarda el inventario, las lecturas, las alertas, las simulaciones y el historial de comandos.

Para cambiar el esquema, crea una migración con el siguiente número disponible. Conserva intactos los archivos de migración que ya se aplicaron. Haz una copia antes de migrar una base con datos que quieras conservar.

Usa una base y credenciales distintas para pruebas y producción. Guarda el respaldo fuera de Git y verifica que puedas restaurarlo. Para validar el esquema desde cero, ejecuta `mvn -Dtest=PostgresSchemaIntegrationTest test` con Docker activo.
