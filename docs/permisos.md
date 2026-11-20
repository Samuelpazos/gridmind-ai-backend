# Access and permissions

Operators manage their own inventory, link sensors, run simulations, and use the available monitoring and maintenance controls. The backend enforces inventory isolation and checks access to each resource.

The bootstrap account configured through `ADMIN_EMAIL` owns the existing inventory and receives telemetry from the current ESP32 token. Keep those settings separate from public registration and the optional demo account.

For a remote review, test the evaluator's sign-in. Disable demo access and public registration if the instance is invitation-only.
