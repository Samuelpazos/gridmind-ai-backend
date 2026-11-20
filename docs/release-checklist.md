# GridMind release verification

Run backend tests with Docker available before deploying. Use a private PostgreSQL instance and confirm that Flyway finishes and GET /health returns HTTP 200.

## Verify the public app

1. Open the frontend from a second network and confirm that it calls the public HTTPS API through the configured origin or proxy.
2. Sign in with a test account. In browser developer tools confirm that the session cookie has Secure and HttpOnly attributes.
3. Sign in with a second test account and confirm that the first account's inventory, readings, simulations, alerts, and maintenance records are absent.
4. In the first account, create a simulated transformer and use **Operaciones** to send a 100 °C test reading. In **Alertas**, check the origin, reading timestamp, and triggering reading under **Fuentes de la respuesta**.
5. Program maintenance, check the displayed equipment and date, and verify that **Deshacer último** removes the corresponding record.

## Verify the ESP32 after the backend is public

Use the public HTTPS endpoint ending in /api/telemetry/readings, the server's matching root CA, and the device token from the backend secret configuration. Load those values into the firmware, open the portal to set Wi-Fi, and link the saved code in the device owner's GridMind workspace.

For the cross-network test, move the ESP32 to a separate 2.4 GHz network and confirm that the UI displays a recent **Hardware** reading. The simulator must be paused for the device owner's workspace.
