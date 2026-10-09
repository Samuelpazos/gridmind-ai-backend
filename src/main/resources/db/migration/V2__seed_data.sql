-- V2__seed_data.sql - Seed initial Substations, Transformers, Sensors, and Readings for GridMind AI Demo

-- Substations
INSERT INTO substations (id, code, name, location, type, status, created_at)
VALUES 
('sub-001', 'SUB-NORTE', 'Subestación Norte', 'Distrito Urbano Norte - Zona Residencial', 'URBAN', 'NORMAL', CURRENT_TIMESTAMP),
('sub-002', 'SUB-CENTRO', 'Subestación Centro', 'Centro Comercial e Industrial Metropolitano', 'INDUSTRIAL', 'WARNING', CURRENT_TIMESTAMP),
('sub-003', 'SUB-SUR', 'Subestación Sur', 'Parque Industrial Sur', 'INDUSTRIAL', 'CRITICAL', CURRENT_TIMESTAMP);

-- Transformers for Subestación Norte (Urban)
INSERT INTO transformers (id, substation_id, code, name, rating_kva, age_years, max_temp_c, max_current_a, status, failure_probability, risk_level, priority_score, created_at)
VALUES 
('trans-001', 'sub-001', 'T-N-001', 'Transformador Residencial Norte 1', 750.0, 3, 85.0, 350.0, 'NORMAL', 0.08, 'LOW', 12.0, CURRENT_TIMESTAMP),
('trans-002', 'sub-001', 'T-N-002', 'Transformador Residencial Norte 2', 1000.0, 6, 85.0, 450.0, 'NORMAL', 0.14, 'LOW', 18.5, CURRENT_TIMESTAMP);

-- Transformers for Subestación Centro (Industrial)
INSERT INTO transformers (id, substation_id, code, name, rating_kva, age_years, max_temp_c, max_current_a, status, failure_probability, risk_level, priority_score, created_at)
VALUES 
('trans-003', 'sub-002', 'T-C-001', 'Transformador Comercial Centro 1', 1500.0, 8, 90.0, 600.0, 'WARNING', 0.45, 'MEDIUM', 54.0, CURRENT_TIMESTAMP),
('trans-004', 'sub-002', 'T-C-002', 'Transformador Comercial Centro 2', 1250.0, 5, 90.0, 500.0, 'NORMAL', 0.12, 'LOW', 21.0, CURRENT_TIMESTAMP);

-- Transformers for Subestación Sur (Industrial)
INSERT INTO transformers (id, substation_id, code, name, rating_kva, age_years, max_temp_c, max_current_a, status, failure_probability, risk_level, priority_score, created_at)
VALUES 
('trans-005', 'sub-003', 'T-S-001', 'Transformador Alta Potencia Sur 1', 2000.0, 14, 95.0, 800.0, 'CRITICAL', 0.78, 'HIGH', 88.5, CURRENT_TIMESTAMP),
('trans-006', 'sub-003', 'T-S-002', 'Transformador Alta Potencia Sur 2', 2000.0, 10, 95.0, 800.0, 'NORMAL', 0.22, 'LOW', 33.0, CURRENT_TIMESTAMP);

-- Sensors for T-N-001 (Normal)
INSERT INTO sensors (id, transformer_id, code, type, unit, status, created_at) VALUES
('sens-001', 'trans-001', 'SN-001-TMP', 'TEMPERATURE', '°C', 'ACTIVE', CURRENT_TIMESTAMP),
('sens-002', 'trans-001', 'SN-001-VLT', 'VOLTAGE', 'kV', 'ACTIVE', CURRENT_TIMESTAMP),
('sens-003', 'trans-001', 'SN-001-CUR', 'CURRENT', 'A', 'ACTIVE', CURRENT_TIMESTAMP),
('sens-004', 'trans-001', 'SN-001-VIB', 'VIBRATION', 'mm/s', 'ACTIVE', CURRENT_TIMESTAMP);

-- Sensors for T-C-001 (Warning)
INSERT INTO sensors (id, transformer_id, code, type, unit, status, created_at) VALUES
('sens-005', 'trans-003', 'SC-001-TMP', 'TEMPERATURE', '°C', 'ACTIVE', CURRENT_TIMESTAMP),
('sens-006', 'trans-003', 'SC-001-VLT', 'VOLTAGE', 'kV', 'ACTIVE', CURRENT_TIMESTAMP),
('sens-007', 'trans-003', 'SC-001-CUR', 'CURRENT', 'A', 'ACTIVE', CURRENT_TIMESTAMP),
('sens-008', 'trans-003', 'SC-001-VIB', 'VIBRATION', 'mm/s', 'ACTIVE', CURRENT_TIMESTAMP);

-- Sensors for T-S-001 (Critical - Reference Demo Scenario Transformer)
INSERT INTO sensors (id, transformer_id, code, type, unit, status, created_at) VALUES
('sens-009', 'trans-005', 'SS-001-TMP', 'TEMPERATURE', '°C', 'ACTIVE', CURRENT_TIMESTAMP),
('sens-010', 'trans-005', 'SS-001-VLT', 'VOLTAGE', 'kV', 'ACTIVE', CURRENT_TIMESTAMP),
('sens-011', 'trans-005', 'SS-001-CUR', 'CURRENT', 'A', 'ACTIVE', CURRENT_TIMESTAMP),
('sens-012', 'trans-005', 'SS-001-VIB', 'VIBRATION', 'mm/s', 'ACTIVE', CURRENT_TIMESTAMP);

-- Initial Sensor Readings for T-N-001 (Normal)
INSERT INTO sensor_readings (id, sensor_id, value, unit, timestamp) VALUES
(gen_random_uuid()::text, 'sens-001', 52.4, '°C', CURRENT_TIMESTAMP - INTERVAL '5 MINUTES'),
(gen_random_uuid()::text, 'sens-002', 13.2, 'kV', CURRENT_TIMESTAMP - INTERVAL '5 MINUTES'),
(gen_random_uuid()::text, 'sens-003', 210.0, 'A', CURRENT_TIMESTAMP - INTERVAL '5 MINUTES'),
(gen_random_uuid()::text, 'sens-004', 1.2, 'mm/s', CURRENT_TIMESTAMP - INTERVAL '5 MINUTES');

-- Initial Sensor Readings for T-C-001 (Warning)
INSERT INTO sensor_readings (id, sensor_id, value, unit, timestamp) VALUES
(gen_random_uuid()::text, 'sens-005', 76.8, '°C', CURRENT_TIMESTAMP - INTERVAL '5 MINUTES'),
(gen_random_uuid()::text, 'sens-006', 13.5, 'kV', CURRENT_TIMESTAMP - INTERVAL '5 MINUTES'),
(gen_random_uuid()::text, 'sens-007', 380.0, 'A', CURRENT_TIMESTAMP - INTERVAL '5 MINUTES'),
(gen_random_uuid()::text, 'sens-008', 3.4, 'mm/s', CURRENT_TIMESTAMP - INTERVAL '5 MINUTES');

-- Initial Sensor Readings for T-S-001 (Critical - Reference Scenario: 87°C, 13.8kV, 420A, Vibración Alta 5.8mm/s)
INSERT INTO sensor_readings (id, sensor_id, value, unit, timestamp) VALUES
(gen_random_uuid()::text, 'sens-009', 87.0, '°C', CURRENT_TIMESTAMP - INTERVAL '1 MINUTE'),
(gen_random_uuid()::text, 'sens-010', 13.8, 'kV', CURRENT_TIMESTAMP - INTERVAL '1 MINUTE'),
(gen_random_uuid()::text, 'sens-011', 420.0, 'A', CURRENT_TIMESTAMP - INTERVAL '1 MINUTE'),
(gen_random_uuid()::text, 'sens-012', 5.8, 'mm/s', CURRENT_TIMESTAMP - INTERVAL '1 MINUTE');

-- Alerts
INSERT INTO alerts (id, transformer_id, severity, message, acknowledged, source, created_at) VALUES
('alt-001', 'trans-003', 'WARNING', 'Temperatura elevada en T-C-001: 76.8 °C (Umbral de aviso: 75.0 °C)', FALSE, 'THRESHOLD_STRATEGY', CURRENT_TIMESTAMP - INTERVAL '30 MINUTES'),
('alt-002', 'trans-005', 'CRITICAL', 'ALERTA DE ALTO RIESGO DE FALLA en T-S-001. Probabilidad: 78.0%. Temperatura (87.0 °C) y Vibración (5.8 mm/s) elevadas.', FALSE, 'SUPERVISED_MODEL', CURRENT_TIMESTAMP - INTERVAL '10 MINUTES');

-- Maintenance Recommendations
INSERT INTO maintenance_recommendations (id, transformer_id, recommendation, status, created_at) VALUES
('rec-001', 'trans-003', 'Revisar ventiladores de enfriamiento y verificar conexiones secundarias.', 'PENDING', CURRENT_TIMESTAMP - INTERVAL '30 MINUTES'),
('rec-002', 'trans-005', 'Inspeccionar el sistema de refrigeración del transformador.', 'SCHEDULED', CURRENT_TIMESTAMP - INTERVAL '10 MINUTES');

-- Initial Prediction for T-S-001
INSERT INTO predictions (id, transformer_id, failure_probability, risk_level, top_factors, explanation, evaluated_at, model_used) VALUES
('pred-001', 'trans-005', 0.78, 'HIGH', '1. Temperatura elevada (87°C); 2. Vibración alta (5.8 mm/s); 3. Edad del transformador (14 años); 4. Corriente elevada (420A)', 'El transformador presenta temperatura y vibración superiores a los valores normales y tiene una antigüedad elevada.', CURRENT_TIMESTAMP - INTERVAL '10 MINUTES', 'SUPERVISED_MODEL_RANDOM_FOREST');

-- Initial History Logs
INSERT INTO history_logs (id, event_type, entity_type, entity_id, description, timestamp) VALUES
(gen_random_uuid()::text, 'SYSTEM_STARTUP', 'SYSTEM', 'GRIDMIND-01', 'Sistema GridMind AI iniciado correctamente. Semilla de datos y modelos cargados.', CURRENT_TIMESTAMP - INTERVAL '1 HOUR'),
(gen_random_uuid()::text, 'PREDICTION_GENERATED', 'TRANSFORMER', 'trans-005', 'Predicción de fallos ejecutada para T-S-001. Probabilidad: 78%, Riesgo: HIGH', CURRENT_TIMESTAMP - INTERVAL '10 MINUTES');
