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
1. **Observer Pattern**: Los sensores notifican eventos a `AnalysisServiceObserver`, `AlertServiceObserver`, e `HistoryRecorderObserver`.
2. **Strategy Pattern**: Intercambio dinámico de estrategias: `ThresholdStrategy`, `IsolationForestStrategy`, `SupervisedModelStrategy`, y `WeightedAgeStrategy`.
3. **State Pattern**: Control riguroso de estados: `NormalState`, `WarningState`, `CriticalState`, y `FailedState`.
4. **Abstract Factory Pattern**: Creación de componentes mediante `UrbanGridFactory` e `IndustrialGridFactory`.
5. **Command & Memento Pattern**: Comandos ejecutables (`SimulateFailureCommand`, `ScheduleMaintenanceCommand`, `AcknowledgeAlertCommand`) con soporte de Undo/Memento vía `CommandInvoker`.
6. **Composite Pattern**: Estructura de componentes `Substation` -> `Transformer` -> `Sensor` con cálculo de peor estado propagado.

## Ejecución Local
```bash
mvn clean package
mvn spring-boot:run
```

Al abrir la aplicación, crea una cuenta desde **Crear cuenta**. La primera cuenta creada recibe el rol **administrador**; las demás reciben el rol **operador**. Usa una contraseña de al menos 12 caracteres. GridMind almacena hashes BCrypt (no contraseñas en texto plano), y solo la cuenta administradora puede cambiar la configuración del simulador o de la estrategia de IA. La sesión vence después de 8 horas y puedes cerrarla desde el botón de salida de la barra lateral.

El registro público y la promoción automática de la primera cuenta están destinados a desarrollo y uso local. Antes de exponer el backend a Internet, desactiva el registro público e implementa controles de administración, HTTPS, protección contra intentos repetidos y verificación de correo.

Para habilitar las explicaciones y predicciones manuales con Claude, crea una clave en [Anthropic Console](https://console.anthropic.com/settings/keys) y configura `CLAUDE_API_KEY` en el entorno antes de iniciar el backend. El flujo automático de análisis sigue usando Smile incluso cuando Claude no está configurado.

En PowerShell puedes solicitar la clave de forma interactiva para no escribirla como parte de un comando guardado:
```powershell
$env:CLAUDE_API_KEY = Read-Host "Pega tu clave de Anthropic"
mvn spring-boot:run
```

La interfaz permite solicitar una predicción manual por transformador con el botón **Predecir con Claude**. También está disponible `POST /api/components/{id}/claude-prediction`; el endpoint anterior `/api/components/{id}/gemini-prediction` se conserva como alias temporal. Se requiere una lectura reciente de cada sensor; la estimación es orientativa y no certificada. No compartas la clave en el chat ni la incluyas en archivos versionados.

### Ingesta experimental desde ESP32

Configura `TELEMETRY_DEVICE_TOKEN` con un secreto aleatorio de al menos 32 caracteres antes de arrancar el backend. El ESP32 envía lotes de 1 a 32 lecturas a `POST /api/telemetry/readings`, incluyendo el secreto en `X-Device-Token`; no necesita una cuenta de GridMind ni una cookie de sesión. El simulador debe estar pausado por una cuenta administradora desde **Operaciones**. Cada lectura requiere un `sensorCode`, `value` y `unit` coincidentes con un sensor activo existente; `timestamp` ISO-8601 es opcional. El endpoint guarda las lecturas como `HARDWARE` y activa la cadena normal de observadores.

Para una prueba local, conecta computador y ESP32 a la misma red privada y usa `http://<IP-local-del-computador>:8080` (no `localhost`). La petición HTTP transmite la credencial sin cifrar: mantenla en una red aislada y no despliegues esta configuración en Internet; para uso externo se requiere HTTPS. Utiliza únicamente una maqueta de baja tensión aislada, nunca la red eléctrica. Antes de usar predicciones como medida de desempeño real, calibra las magnitudes y reúne mediciones representativas etiquetadas: el modelo actual fue entrenado con datos sintéticos.

Documentación Swagger interactiva disponible en: `http://localhost:8080/swagger-ui.html`
WebSocket endpoint disponible en: `ws://localhost:8080/ws/stream`

### Comunicación con un frontend desplegado por separado

El frontend llama a la API REST bajo `/api` y conserva las credenciales de sesión. En el build del frontend, configura `VITE_API_BASE_URL` con la URL pública del backend terminada en `/api` (por ejemplo, `https://api.example.com/api`). En el backend, configura `CORS_ALLOWED_ORIGIN_PATTERNS` con el origen exacto del frontend (por ejemplo, `https://gridmind.example.com`); se permiten varios orígenes separados por comas. La misma lista se aplica a WebSocket. Mantén HTTPS y no uses comodines de origen en producción.
