ALTER TABLE alerts
    ADD COLUMN reading_id VARCHAR(36) REFERENCES sensor_readings (id) ON DELETE SET NULL,
    ADD COLUMN reading_source VARCHAR(20),
    ADD COLUMN reading_timestamp TIMESTAMPTZ;

CREATE INDEX idx_alerts_reading_id ON alerts (reading_id) WHERE reading_id IS NOT NULL;
