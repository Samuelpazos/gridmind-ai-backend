# Hardware telemetry

The ESP32 sends a JSON array to `POST /api/telemetry/readings`. Each item identifies a registered sensor by `sensorCode` and includes a numeric `value` and its `unit`. The device authenticates with the `X-Device-Token` header.

Link the exact firmware sensor code in **Activos**. Pause the data simulator for the device owner under **Operaciones → Entrada de telemetría** before sending hardware readings. The app labels readings by source so you can verify that hardware telemetry is arriving.

A successful response means the batch was processed. If readings are missing, check the sensor code, device token, simulator status, and backend connectivity.
