# Backend API acceptance tests

Use a disposable application and PostgreSQL database. Configure a test telemetry owner and token, a linked temperature sensor, and a second workspace. Start each telemetry case with the owner's simulator paused unless the steps say otherwise. Send JSON arrays to `POST /api/telemetry/readings` with the test value of `X-Device-Token`. An ordinary valid item contains the linked `sensorCode`, a numeric `value` and `unit: "°C"`. For demonstration items add `demonstrationChannel` and its corresponding unit. Compare the reading count before and after negative cases.

## BE-05 · Invalid token at the HTTP boundary

Send a valid temperature payload with an incorrect device token. Expect HTTP 401 and no new row for the sensor.
