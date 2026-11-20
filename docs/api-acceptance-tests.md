# Backend API acceptance tests

Use a disposable application and PostgreSQL database. Configure a test telemetry owner and token, a linked temperature sensor, and a second workspace. Start each telemetry case with the owner's simulator paused unless the steps say otherwise. Send JSON arrays to `POST /api/telemetry/readings` with the test value of `X-Device-Token`. An ordinary valid item contains the linked `sensorCode`, a numeric `value` and `unit: "°C"`. For demonstration items add `demonstrationChannel` and its corresponding unit. Compare the reading count before and after negative cases.

## BE-05 · Invalid token at the HTTP boundary

Send a valid temperature payload with an incorrect device token. Expect HTTP 401 and no new row for the sensor.

## BE-06 · Foreign workspace sensor

Send a payload for a hardware sensor owned by another account. Expect HTTP 404 and no reading in either workspace.

## BE-07 · Archived transformer

Archive a disposable real transformer and send a temperature reading for its former sensor. Expect HTTP 404.

## BE-08 · Oversized batch

Send an array of 33 structurally valid items. Expect HTTP 400 and no change to the reading count.

## BE-09 · Incorrect measurement unit

Send a temperature item using `kV`. Expect HTTP 422 and no new reading.

## BE-10 · Out-of-range sensor magnitude

Send a value with absolute magnitude greater than 1,000,000. Expect HTTP 400.

## BE-11 · Timestamp ahead of server time

Send a reading dated two minutes after the API server clock. Expect HTTP 400.

## BE-12 · Atomic rejection of a mixed batch

Send a valid temperature item and a voltage demonstration item with the wrong unit. Expect HTTP 422 and verify that neither item was persisted.

## BE-13 · Temperature-only firmware

Send a single normal DS18B20 item without `demonstrationChannel`. Expect HTTP 201 and `source=HARDWARE`.

## BE-14 · Four-channel firmware

Send a temperature item and demonstration items for voltage, current and vibration using its linked temperature code. Expect HTTP 201 and four readings with the configured units.

## BE-15 · Reused demonstration sensor IDs

Submit two valid temperature-and-voltage batches for the same anchor at different times. Compare the returned voltage sensor IDs and confirm they match.

## BE-16 · Persisted provenance

After a valid mixed-origin batch, query its sensor readings by timestamp in the disposable database. Confirm temperature is `HARDWARE` and the other channels are `HARDWARE_DEMO`.

## BE-17 · Demonstration values outside operational alerts

Start with normal temperature, then submit extreme potentiometer values in an otherwise valid batch. Check that the demonstration channels do not generate operational alerts.

## BE-18 · Paused simulator permits hardware

Pause the simulator for the configured telemetry owner and send a valid item. Expect HTTP 201.

## BE-19 · Active simulator blocks hardware

Activate the simulator for the telemetry owner, then try a valid hardware item. Expect HTTP 409. Restore the original simulator setting.

## BE-20 · Independent simulator workspace

Keep the telemetry owner's simulator paused and start simulation in a second workspace. Send the owner's valid hardware batch and expect HTTP 201.

## BE-21 · Workspace-scoped last hardware reading

In the owner's session, read telemetry status after a known hardware batch. Verify the timestamp and confirm a different account cannot see that reading.
