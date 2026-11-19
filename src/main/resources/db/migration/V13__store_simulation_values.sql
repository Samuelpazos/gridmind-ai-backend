ALTER TABLE simulations
    ALTER COLUMN delta_percent DROP NOT NULL,
    ADD COLUMN original_value DOUBLE PRECISION,
    ADD COLUMN simulated_value DOUBLE PRECISION;
