# Telemetría de hardware

El ESP32 envía un arreglo JSON a `POST /api/telemetry/readings`. Cada elemento identifica un sensor registrado por `sensorCode` y contiene un `value` numérico y su `unit`. El dispositivo se autentica mediante el encabezado `X-Device-Token`.

Vincula primero el código exacto del firmware en **Activos**. Pausa el simulador de datos desde **Operaciones → Entrada de telemetría** para recibir datos físicos. La aplicación distingue el origen de las lecturas para poder comprobar la conexión.

Una respuesta exitosa indica que el lote fue procesado. Si no aparecen lecturas, comprueba el código del sensor, la coincidencia del token y el estado del simulador de datos.
