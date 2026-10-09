-- V1__init_schema.sql - GridMind AI Schema Initialization

CREATE TABLE substations (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(150),
    type VARCHAR(30) NOT NULL, -- URBAN, INDUSTRIAL
    status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transformers (
    id VARCHAR(36) PRIMARY KEY,
    substation_id VARCHAR(36) NOT NULL REFERENCES substations(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    rating_kva DOUBLE PRECISION NOT NULL DEFAULT 1000.0,
    age_years INT NOT NULL DEFAULT 5,
    max_temp_c DOUBLE PRECISION NOT NULL DEFAULT 90.0,
    max_current_a DOUBLE PRECISION NOT NULL DEFAULT 500.0,
    status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    failure_probability DOUBLE PRECISION NOT NULL DEFAULT 0.05,
    risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW',
    priority_score DOUBLE PRECISION NOT NULL DEFAULT 5.0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sensors (
    id VARCHAR(36) PRIMARY KEY,
    transformer_id VARCHAR(36) NOT NULL REFERENCES transformers(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(30) NOT NULL, -- TEMPERATURE, VOLTAGE, CURRENT, VIBRATION
    unit VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sensor_readings (
    id VARCHAR(36) PRIMARY KEY,
    sensor_id VARCHAR(36) NOT NULL REFERENCES sensors(id) ON DELETE CASCADE,
    value DOUBLE PRECISION NOT NULL,
    unit VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_readings_sensor_time ON sensor_readings(sensor_id, timestamp DESC);

CREATE TABLE anomalies (
    id VARCHAR(36) PRIMARY KEY,
    transformer_id VARCHAR(36) NOT NULL REFERENCES transformers(id) ON DELETE CASCADE,
    anomaly_score DOUBLE PRECISION NOT NULL,
    detected_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    strategy_used VARCHAR(50) NOT NULL,
    details TEXT
);

CREATE TABLE predictions (
    id VARCHAR(36) PRIMARY KEY,
    transformer_id VARCHAR(36) NOT NULL REFERENCES transformers(id) ON DELETE CASCADE,
    failure_probability DOUBLE PRECISION NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    top_factors TEXT,
    explanation TEXT,
    evaluated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    model_used VARCHAR(50) NOT NULL
);

CREATE TABLE alerts (
    id VARCHAR(36) PRIMARY KEY,
    transformer_id VARCHAR(36) NOT NULL REFERENCES transformers(id) ON DELETE CASCADE,
    severity VARCHAR(20) NOT NULL, -- WARNING, CRITICAL, FAILED
    message TEXT NOT NULL,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_by VARCHAR(100),
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    source VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE maintenance_recommendations (
    id VARCHAR(36) PRIMARY KEY,
    transformer_id VARCHAR(36) NOT NULL REFERENCES transformers(id) ON DELETE CASCADE,
    recommendation TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SCHEDULED, COMPLETED
    scheduled_date TIMESTAMP WITH TIME ZONE,
    completion_date TIMESTAMP WITH TIME ZONE,
    result_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE simulations (
    id VARCHAR(36) PRIMARY KEY,
    transformer_id VARCHAR(36) NOT NULL REFERENCES transformers(id) ON DELETE CASCADE,
    variable_modified VARCHAR(50) NOT NULL,
    delta_percent DOUBLE PRECISION NOT NULL,
    original_risk VARCHAR(20) NOT NULL,
    simulated_risk VARCHAR(20) NOT NULL,
    original_probability DOUBLE PRECISION NOT NULL,
    simulated_probability DOUBLE PRECISION NOT NULL,
    original_state VARCHAR(20) NOT NULL,
    simulated_state VARCHAR(20) NOT NULL,
    recommendation TEXT,
    executed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE command_logs (
    id VARCHAR(36) PRIMARY KEY,
    command_name VARCHAR(100) NOT NULL,
    target_id VARCHAR(36),
    parameters TEXT,
    executed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    executed_by VARCHAR(100) DEFAULT 'OPERATOR',
    undoable BOOLEAN DEFAULT FALSE
);

CREATE TABLE history_logs (
    id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(36) NOT NULL,
    description TEXT NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
