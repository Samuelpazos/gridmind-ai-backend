-- V5__operational_catalogs_and_maintenance.sql
-- Extends the operational model to 30 application tables.

ALTER TABLE user_accounts
    ADD CONSTRAINT chk_user_display_name_length
    CHECK (char_length(btrim(display_name)) BETWEEN 1 AND 12);

CREATE TABLE roles (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE permissions (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT
);

CREATE TABLE role_permissions (
    role_id VARCHAR(36) NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id VARCHAR(36) NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_roles (
    user_id VARCHAR(36) NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
    role_id VARCHAR(36) NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE transformer_models (
    id VARCHAR(36) PRIMARY KEY,
    manufacturer VARCHAR(100) NOT NULL,
    model_code VARCHAR(100) NOT NULL UNIQUE,
    rated_power_kva DOUBLE PRECISION NOT NULL,
    nominal_voltage_kv DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (rated_power_kva > 0),
    CHECK (nominal_voltage_kv > 0)
);

CREATE TABLE sensor_types (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    default_unit VARCHAR(20) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE transformers
    ADD COLUMN model_id VARCHAR(36) REFERENCES transformer_models(id) ON DELETE SET NULL;

ALTER TABLE sensors
    ADD COLUMN sensor_type_id VARCHAR(36) REFERENCES sensor_types(id) ON DELETE SET NULL;

CREATE TABLE sensor_calibrations (
    id VARCHAR(36) PRIMARY KEY,
    sensor_id VARCHAR(36) NOT NULL REFERENCES sensors(id) ON DELETE CASCADE,
    calibrated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    performed_by VARCHAR(100) NOT NULL,
    result VARCHAR(20) NOT NULL DEFAULT 'PASSED',
    certificate_number VARCHAR(100),
    notes TEXT,
    CHECK (expires_at IS NULL OR expires_at > calibrated_at),
    CHECK (result IN ('PASSED', 'FAILED', 'PENDING'))
);

CREATE TABLE sensor_thresholds (
    id VARCHAR(36) PRIMARY KEY,
    sensor_id VARCHAR(36) NOT NULL REFERENCES sensors(id) ON DELETE CASCADE,
    warning_min DOUBLE PRECISION,
    warning_max DOUBLE PRECISION,
    critical_min DOUBLE PRECISION,
    critical_max DOUBLE PRECISION,
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK (warning_min IS NULL OR critical_min IS NULL OR critical_min <= warning_min),
    CHECK (warning_max IS NULL OR critical_max IS NULL OR warning_max <= critical_max)
);

CREATE TABLE sensor_reading_batches (
    id VARCHAR(36) PRIMARY KEY,
    source VARCHAR(20) NOT NULL,
    reading_count INTEGER NOT NULL DEFAULT 0,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    error_message TEXT,
    CHECK (reading_count >= 0),
    CHECK (source IN ('SIMULATED', 'HARDWARE')),
    CHECK (status IN ('RECEIVED', 'PROCESSED', 'FAILED'))
);

CREATE TABLE alert_rules (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    sensor_type VARCHAR(30),
    severity VARCHAR(20) NOT NULL,
    condition_expression TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (severity IN ('WARNING', 'CRITICAL', 'FAILED'))
);

CREATE TABLE alert_notifications (
    id VARCHAR(36) PRIMARY KEY,
    alert_id VARCHAR(36) NOT NULL REFERENCES alerts(id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL,
    recipient VARCHAR(254) NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    CHECK (channel IN ('EMAIL', 'SMS', 'WEBHOOK', 'IN_APP')),
    CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE TABLE maintenance_plans (
    id VARCHAR(36) PRIMARY KEY,
    transformer_id VARCHAR(36) NOT NULL REFERENCES transformers(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    frequency_days INTEGER NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (frequency_days > 0),
    CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE TABLE maintenance_tasks (
    id VARCHAR(36) PRIMARY KEY,
    plan_id VARCHAR(36) NOT NULL REFERENCES maintenance_plans(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    instructions TEXT NOT NULL,
    estimated_minutes INTEGER,
    sequence_number INTEGER NOT NULL DEFAULT 1,
    CHECK (estimated_minutes IS NULL OR estimated_minutes > 0),
    CHECK (sequence_number > 0)
);

CREATE TABLE maintenance_technicians (
    id VARCHAR(36) PRIMARY KEY,
    employee_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(254),
    phone VARCHAR(40),
    specialization VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE work_orders (
    id VARCHAR(36) PRIMARY KEY,
    transformer_id VARCHAR(36) NOT NULL REFERENCES transformers(id) ON DELETE CASCADE,
    maintenance_plan_id VARCHAR(36) REFERENCES maintenance_plans(id) ON DELETE SET NULL,
    technician_id VARCHAR(36) REFERENCES maintenance_technicians(id) ON DELETE SET NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    scheduled_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (status IN ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CHECK (completed_at IS NULL OR scheduled_at IS NULL OR completed_at >= scheduled_at)
);

CREATE TABLE equipment_documents (
    id VARCHAR(36) PRIMARY KEY,
    transformer_id VARCHAR(36) REFERENCES transformers(id) ON DELETE CASCADE,
    substation_id VARCHAR(36) REFERENCES substations(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    storage_uri TEXT NOT NULL,
    version VARCHAR(30) NOT NULL DEFAULT '1',
    uploaded_by VARCHAR(100) NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK ((transformer_id IS NOT NULL) <> (substation_id IS NOT NULL))
);

CREATE TABLE audit_events (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) REFERENCES user_accounts(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(36),
    details TEXT,
    ip_address INET,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sensor_calibrations_sensor_date
    ON sensor_calibrations(sensor_id, calibrated_at DESC);
CREATE INDEX idx_sensor_thresholds_sensor_active
    ON sensor_thresholds(sensor_id, active);
CREATE INDEX idx_transformers_model
    ON transformers(model_id);
CREATE INDEX idx_sensors_sensor_type
    ON sensors(sensor_type_id);
CREATE INDEX idx_alert_notifications_alert
    ON alert_notifications(alert_id);
CREATE INDEX idx_work_orders_transformer_status
    ON work_orders(transformer_id, status);
CREATE INDEX idx_audit_events_entity_date
    ON audit_events(entity_type, entity_id, occurred_at DESC);

INSERT INTO roles (id, code, name, description) VALUES
    ('role-operator', 'OPERATOR', 'Operator', 'Operates the monitoring console'),
    ('role-maintenance', 'MAINTENANCE', 'Maintenance', 'Manages maintenance work orders'),
    ('role-admin', 'ADMIN', 'Administrator', 'Manages users and system configuration')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (id, code, name, description) VALUES
    ('perm-read-monitoring', 'MONITORING_READ', 'Read monitoring data', 'View substations, transformers, sensors, readings, and alerts'),
    ('perm-manage-maintenance', 'MAINTENANCE_MANAGE', 'Manage maintenance', 'Create and update maintenance plans and work orders'),
    ('perm-manage-users', 'USERS_MANAGE', 'Manage users', 'Manage accounts, roles, and permissions'),
    ('perm-manage-configuration', 'CONFIGURATION_MANAGE', 'Manage configuration', 'Manage alert rules, thresholds, and system configuration')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON (
    (r.code IN ('OPERATOR', 'MAINTENANCE', 'ADMIN') AND p.code = 'MONITORING_READ')
    OR (r.code IN ('MAINTENANCE', 'ADMIN') AND p.code = 'MAINTENANCE_MANAGE')
    OR (r.code = 'ADMIN' AND p.code IN ('USERS_MANAGE', 'CONFIGURATION_MANAGE'))
)
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM user_accounts u
JOIN roles r ON r.code = u.role::text
ON CONFLICT DO NOTHING;

CREATE OR REPLACE FUNCTION sync_user_role_assignment()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM user_roles
    WHERE user_id = NEW.id
      AND role_id <> (
          SELECT r.id
          FROM roles r
          WHERE r.code = NEW.role::text
      );

    INSERT INTO user_roles (user_id, role_id)
    SELECT NEW.id, r.id
    FROM roles r
    WHERE r.code = NEW.role::text
    ON CONFLICT (user_id, role_id) DO NOTHING;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_sync_user_role_assignment
AFTER INSERT OR UPDATE OF role ON user_accounts
FOR EACH ROW
EXECUTE FUNCTION sync_user_role_assignment();

INSERT INTO sensor_types (id, code, name, default_unit, description) VALUES
    ('stype-temperature', 'TEMPERATURE', 'Temperature', '°C', 'Transformer temperature'),
    ('stype-voltage', 'VOLTAGE', 'Voltage', 'kV', 'Electrical voltage'),
    ('stype-current', 'CURRENT', 'Current', 'A', 'Electrical current'),
    ('stype-vibration', 'VIBRATION', 'Vibration', 'mm/s', 'Mechanical vibration')
ON CONFLICT (code) DO NOTHING;

UPDATE sensors s
SET sensor_type_id = st.id
FROM sensor_types st
WHERE st.code = s.type
  AND s.sensor_type_id IS NULL;
