ALTER TABLE sensor_readings
    DROP CONSTRAINT IF EXISTS sensor_readings_source_check;

UPDATE sensor_readings
SET source = 'SIMULATION'
WHERE source = 'SIMULATED';

ALTER TABLE sensor_readings
    ALTER COLUMN source SET DEFAULT 'SIMULATION',
    ADD CONSTRAINT sensor_readings_source_check
        CHECK (source IN ('SIMULATION', 'HARDWARE'));

CREATE INDEX IF NOT EXISTS idx_sensor_readings_source_timestamp
    ON sensor_readings (source, timestamp DESC);
