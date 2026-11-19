# Desplegar la API

- Ejecuta la API Spring Boot con Java 25 y una instancia privada de PostgreSQL.
- Configura `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` en el proveedor.
- Activa `SPRING_PROFILES_ACTIVE=production`. El proxy público debe terminar TLS y sobrescribir `X-Forwarded-Proto` con el esquema original.
- Configura `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `DEMO_MODE=false` y `AUTH_REGISTRATION_ENABLED=false` para una instancia de evaluación privada.
- Mantén `TELEMETRY_DEVICE_TOKEN` y las claves de servicios externos en la configuración secreta del proveedor.

Antes de compartir la URL, verifica `GET /health`, inicia sesión como administrador por HTTPS y comprueba los atributos Secure y HttpOnly de la cookie de sesión.
