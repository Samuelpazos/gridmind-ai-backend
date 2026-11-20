ALTER TABLE sensor_readings
    DROP CONSTRAINT sensor_readings_source_check,
    ADD CONSTRAINT sensor_readings_source_check
        CHECK (source IN ('SIMULATION', 'HARDWARE', 'HARDWARE_DEMO'));
