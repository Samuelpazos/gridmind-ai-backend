CREATE INDEX idx_anomalies_detected_at
    ON anomalies(detected_at);

CREATE INDEX idx_predictions_transformer_evaluated
    ON predictions(transformer_id, evaluated_at DESC);

CREATE INDEX idx_predictions_evaluated_at
    ON predictions(evaluated_at);

CREATE INDEX idx_sensor_readings_timestamp
    ON sensor_readings(timestamp);
