# GridMind AI - Backend Service

Backend en Java 25 / Spring Boot 3 para el monitoreo inteligente y predicción de fallos en redes eléctricas simuladas.

## Tecnologías Utilizadas
- **Java 25**
- **Spring Boot 3.5.16** (Spring Web, Data JPA, Validation, WebSocket, Test)
- **Spring Security** (sesión de servidor, protección CSRF y autorización por roles)
- **PostgreSQL 16** & **Flyway Migrations**
- **Smile ML** (Machine Learning Nativo en Java: Random Forest e Isolation Forest)
- **Anthropic Claude API** para explicaciones opcionales de alertas y estimaciones manuales de riesgo (requiere `CLAUDE_API_KEY`; la clave permanece en el backend)
- **Springdoc OpenAPI / Swagger UI** (v2.8.17)
- **Lombok & JUnit 5 / Mockito**

## Arquitectura por Capas & Patrones de Diseño
1. **Observer Pattern**: Los sensores notifican eventos a `AnalysisServiceObserver`, `AlertServiceObserver`, `HistoryRecorderObserver` y `AuditObserver`; `SensorSubject` registra fallos de observación con SLF4J.
2. **Strategy Pattern**: Intercambio dinámico de estrategias: `ThresholdStrategy`, `IsolationForestStrategy`, `SupervisedModelStrategy`, y `WeightedAgeStrategy`.
3. **State Pattern**: Control riguroso de estados: `NormalState`, `WarningState`, `CriticalState`, y `FailedState`.
4. **Abstract Factory Pattern**: `GridProvisioningService` crea jerarquías con `UrbanGridFactory` o `IndustrialGridFactory`; `POST /api/grid/substations` y `POST /api/grid/transformers` requieren rol ADMIN. El simulador emite lecturas solo para transformadores `TEST`, no para equipos `HARDWARE`.
5. **Command & Memento Pattern**: Los tres comandos guardan snapshots en `command_snapshots`; el undo persistente se limita al usuario autenticado.
6. **Composite Pattern**: `SubstationNode` -> `TransformerNode` -> `SensorNode` agrega estado, riesgo y cantidad de componentes.
7. **Facade & Builder Patterns**: `GridDashboardFacade` consolida los datos del dashboard y `DashboardOverviewDto.Builder` arma la respuesta.
8. **Chain of Responsibility Pattern**: `ReadingHandler` es el punto común de validación, persistencia y notificación para telemetría física y simulada; `TelemetryValidationChain` encadena los validadores.

## Ejecución Local
```bash
mvn clean package
mvn spring-boot:run
```

La contraseña de la base de datos no tiene un valor predeterminado en `application.properties`; al ejecutar el backend fuera de Docker, configura `SPRING_DATASOURCE_PASSWORD` (y, si corresponde, `SPRING_DATASOURCE_URL` y `SPRING_DATASOURCE_USERNAME`). No uses una contraseña real dentro de comandos guardados o archivos versionados.

El registro público crea cuentas **OPERATOR**, nunca promueve automáticamente una cuenta a administradora. Para crear cuentas de bootstrap usa `ADMIN_EMAIL`/`ADMIN_PASSWORD` y `DEMO_EMAIL`/`DEMO_PASSWORD`; `DEMO_MODE=false` desactiva la creación de DEMO. DEMO puede simular fallas, cambiar escenarios y estrategia, ejecutar comandos y completar mantenimientos, pero no gestionar usuarios, crear subestaciones ni editar catálogos. Las contraseñas de bootstrap deben tener entre 12 y 72 caracteres. Si la cuenta configurada ya existe con ese rol, se conservan sus credenciales guardadas. Para cerrar el registro público establece `AUTH_REGISTRATION_ENABLED=false`. Los intentos de inicio de sesión se limitan por IP (5 fallos en 15 minutos por defecto). Las contraseñas se almacenan con BCrypt y la sesión vence después de 8 horas.

`docker-compose.yml` reenvía las variables de bootstrap y `CLAUDE_API_KEY` al backend. Copia `.env.example` a `.env` y reemplaza las credenciales de ejemplo antes de usarlo fuera de una máquina local.

Para habilitar las explicaciones y predicciones manuales con Claude, crea una clave en [Anthropic Console](https://console.anthropic.com/settings/keys) y configura `CLAUDE_API_KEY` en el entorno antes de iniciar el backend. El flujo automático de análisis sigue usando Smile incluso cuando Claude no está configurado.
El límite de Claude se configura con `CLAUDE_MAX_REQUESTS_PER_DAY` (20 por defecto). Es un presupuesto por proceso en memoria: se restablece al cambiar el día UTC o al reiniciar la instancia.

En PowerShell puedes solicitar la clave de forma interactiva para no escribirla como parte de un comando guardado:
```powershell
$env:CLAUDE_API_KEY = Read-Host "Pega tu clave de Anthropic"
mvn spring-boot:run
```

La interfaz permite solicitar una predicción manual por transformador con el botón **Predecir con Claude**. También está disponible `POST /api/components/{id}/claude-prediction`; el endpoint anterior `/api/components/{id}/gemini-prediction` se conserva como alias temporal. Se requiere una lectura reciente de cada sensor; la estimación es orientativa y no certificada. No compartas la clave en el chat ni la incluyas en archivos versionados.

### Persistencia, migraciones y retención

Flyway carga exclusivamente las migraciones versionadas de `src/main/resources/db/migration` (actualmente V1–V9). La V9 elimina tablas sin entidad ni uso de aplicación; las copias bajo `database/migrations` son históricas y no deben usarse para nuevas migraciones ni modificarse como sustituto del historial Flyway.

La retención de lecturas de sensores, predicciones y anomalías está activada por defecto durante siete días. Puedes cambiar el plazo con `GRIDMIND_RETENTION_DAYS`; una tarea diaria elimina esos tres tipos de registros anteriores al plazo. No elimina alertas ni eventos de auditoría. La integración PostgreSQL/Testcontainers verifica la cadena de migraciones, la validación del esquema JPA y la tarea de retención; requiere Docker y se omite automáticamente si no está disponible.

Los últimos tests unitarios se ejecutan con:
```bash
mvn test
```

### Ingesta experimental desde ESP32

Configura `TELEMETRY_DEVICE_TOKEN` con un secreto aleatorio de al menos 32 caracteres antes de arrancar el backend. El ESP32 envía lotes de 1 a 32 lecturas a `POST /api/telemetry/readings`, incluyendo el secreto en `X-Device-Token`; no necesita una cuenta de GridMind ni una cookie de sesión. El simulador debe estar pausado por una cuenta administradora desde **Operaciones**. Cada lectura requiere un `sensorCode`, `value` y `unit` coincidentes con un sensor activo existente; `timestamp` ISO-8601 es opcional. El endpoint guarda las lecturas como `HARDWARE` y activa la cadena normal de observadores.

Para una prueba local, conecta computador y ESP32 a la misma red privada y usa `http://<IP-local-del-computador>:8080` (no `localhost`). La petición HTTP transmite la credencial sin cifrar; úsala solo en una red aislada. Para un backend público, el firmware debe usar la URL HTTPS y validar el certificado raíz del servidor; configura también el mismo `TELEMETRY_DEVICE_TOKEN` en el backend y en el ESP32. Utiliza únicamente una maqueta de baja tensión aislada, nunca la red eléctrica. Antes de usar predicciones como medida de desempeño real, calibra las magnitudes y reúne mediciones representativas etiquetadas: el modelo actual fue entrenado con datos sintéticos.

Documentación Swagger interactiva disponible en: `http://localhost:8080/swagger-ui.html`
WebSocket endpoint disponible en: `ws://localhost:8080/ws/stream`

### Comunicación con un frontend desplegado por separado

El frontend llama a la API REST bajo `/api` y conserva las credenciales de sesión. En el build del frontend, configura `VITE_API_BASE_URL` con la URL pública del backend terminada en `/api` (por ejemplo, `https://api.example.com/api`). En el backend, configura `CORS_ALLOWED_ORIGIN_PATTERNS` con el origen exacto del frontend (por ejemplo, `https://gridmind.example.com`); se permiten varios orígenes separados por comas. La misma lista se aplica a WebSocket. Mantén HTTPS y no uses comodines de origen en producción.

## Guías

- [documentar el despliegue HTTPS del backend](docs/despliegue.md)
- [explicar las migraciones de PostgreSQL](docs/postgresql.md)
- [describir permisos de operador, administración y demo](docs/permisos.md)
- [documentar el contrato de telemetría](docs/telemetria.md)
- [registrar comprobaciones del backend para la entrega](docs/verificacion.md)
