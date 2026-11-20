ALTER TABLE substations ADD COLUMN owner_account_id VARCHAR(36) REFERENCES user_accounts(id);
CREATE INDEX idx_substations_owner ON substations(owner_account_id);
ALTER TABLE sensor_reading_batches ADD COLUMN owner_account_id VARCHAR(36) REFERENCES user_accounts(id);
CREATE INDEX idx_reading_batches_owner ON sensor_reading_batches(owner_account_id);

-- Browser requests switch to this non-owner role before issuing application queries.
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'gridmind_workspace') THEN
        CREATE ROLE gridmind_workspace NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS;
    END IF;
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'gridmind_workspace'
        AND (rolsuper OR rolbypassrls OR rolcreaterole OR rolcanlogin OR rolinherit)) THEN
        RAISE EXCEPTION 'gridmind_workspace must be a restricted, non-login role';
    END IF;
    EXECUTE format('GRANT gridmind_workspace TO %I', session_user);
END $$;

GRANT USAGE ON SCHEMA public TO gridmind_workspace;
GRANT SELECT ON transformer_models, sensor_types, alert_rules, maintenance_technicians TO gridmind_workspace;
GRANT SELECT, INSERT, UPDATE, DELETE ON substations, transformers, sensors, sensor_readings, command_logs, command_snapshots, equipment_documents, history_logs, audit_events, sensor_reading_batches, anomalies, predictions, alerts, maintenance_recommendations, simulations, sensor_calibrations, sensor_thresholds TO gridmind_workspace;

ALTER TABLE substations ENABLE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON substations AS RESTRICTIVE TO gridmind_workspace USING (owner_account_id = current_setting('gridmind.account_id', true));
CREATE POLICY workspace_access ON substations TO gridmind_workspace USING (true);

ALTER TABLE transformers ENABLE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON transformers AS RESTRICTIVE TO gridmind_workspace USING (EXISTS (SELECT 1 FROM substations s WHERE s.id = substation_id));
CREATE POLICY workspace_access ON transformers TO gridmind_workspace USING (true);

ALTER TABLE sensors ENABLE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON sensors AS RESTRICTIVE TO gridmind_workspace USING (EXISTS (SELECT 1 FROM transformers t WHERE t.id = transformer_id));
CREATE POLICY workspace_access ON sensors TO gridmind_workspace USING (true);

ALTER TABLE sensor_readings ENABLE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON sensor_readings AS RESTRICTIVE TO gridmind_workspace USING (EXISTS (SELECT 1 FROM sensors s WHERE s.id = sensor_id));
CREATE POLICY workspace_access ON sensor_readings TO gridmind_workspace USING (true);

ALTER TABLE command_logs ENABLE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON command_logs AS RESTRICTIVE TO gridmind_workspace USING (executed_by = current_setting('gridmind.account_email', true));
CREATE POLICY workspace_access ON command_logs TO gridmind_workspace USING (true);

ALTER TABLE command_snapshots ENABLE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON command_snapshots AS RESTRICTIVE TO gridmind_workspace USING (EXISTS (SELECT 1 FROM command_logs c WHERE c.id = command_log_id));
CREATE POLICY workspace_access ON command_snapshots TO gridmind_workspace USING (true);

ALTER TABLE equipment_documents ENABLE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON equipment_documents AS RESTRICTIVE TO gridmind_workspace USING ((transformer_id IS NOT NULL OR substation_id IS NOT NULL)
    AND (transformer_id IS NULL OR EXISTS (SELECT 1 FROM transformers t WHERE t.id = equipment_documents.transformer_id))
    AND (substation_id IS NULL OR EXISTS (SELECT 1 FROM substations s WHERE s.id = equipment_documents.substation_id)));
CREATE POLICY workspace_access ON equipment_documents TO gridmind_workspace USING (true);

ALTER TABLE history_logs ENABLE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON history_logs AS RESTRICTIVE TO gridmind_workspace USING ((entity_type = 'SENSOR' AND EXISTS (SELECT 1 FROM sensors s WHERE s.id = entity_id))
    OR (entity_type = 'TRANSFORMER' AND EXISTS (SELECT 1 FROM transformers t WHERE t.id = entity_id)));
CREATE POLICY workspace_access ON history_logs TO gridmind_workspace USING (true);

ALTER TABLE audit_events ENABLE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON audit_events AS RESTRICTIVE TO gridmind_workspace USING ((entity_type = 'SENSOR' AND EXISTS (SELECT 1 FROM sensors s WHERE s.id = entity_id))
    OR (entity_type = 'TRANSFORMER' AND EXISTS (SELECT 1 FROM transformers t WHERE t.id = entity_id)));
CREATE POLICY workspace_access ON audit_events TO gridmind_workspace USING (true);

ALTER TABLE sensor_reading_batches ENABLE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON sensor_reading_batches AS RESTRICTIVE TO gridmind_workspace USING (owner_account_id = current_setting('gridmind.account_id', true));
CREATE POLICY workspace_access ON sensor_reading_batches TO gridmind_workspace USING (true);

ALTER TABLE anomalies ENABLE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON anomalies AS RESTRICTIVE TO gridmind_workspace USING (EXISTS (SELECT 1 FROM transformers t WHERE t.id = transformer_id));
CREATE POLICY workspace_access ON anomalies TO gridmind_workspace USING (true);

ALTER TABLE predictions ENABLE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON predictions AS RESTRICTIVE TO gridmind_workspace USING (EXISTS (SELECT 1 FROM transformers t WHERE t.id = transformer_id));
CREATE POLICY workspace_access ON predictions TO gridmind_workspace USING (true);

ALTER TABLE alerts ENABLE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON alerts AS RESTRICTIVE TO gridmind_workspace USING (EXISTS (SELECT 1 FROM transformers t WHERE t.id = transformer_id));
CREATE POLICY workspace_access ON alerts TO gridmind_workspace USING (true);

ALTER TABLE maintenance_recommendations ENABLE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON maintenance_recommendations AS RESTRICTIVE TO gridmind_workspace USING (EXISTS (SELECT 1 FROM transformers t WHERE t.id = transformer_id));
CREATE POLICY workspace_access ON maintenance_recommendations TO gridmind_workspace USING (true);

ALTER TABLE simulations ENABLE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON simulations AS RESTRICTIVE TO gridmind_workspace USING (EXISTS (SELECT 1 FROM transformers t WHERE t.id = transformer_id));
CREATE POLICY workspace_access ON simulations TO gridmind_workspace USING (true);

ALTER TABLE sensor_calibrations ENABLE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON sensor_calibrations AS RESTRICTIVE TO gridmind_workspace USING (EXISTS (SELECT 1 FROM sensors s WHERE s.id = sensor_id));
CREATE POLICY workspace_access ON sensor_calibrations TO gridmind_workspace USING (true);

ALTER TABLE sensor_thresholds ENABLE ROW LEVEL SECURITY;
CREATE POLICY workspace_scope ON sensor_thresholds AS RESTRICTIVE TO gridmind_workspace USING (EXISTS (SELECT 1 FROM sensors s WHERE s.id = sensor_id));
CREATE POLICY workspace_access ON sensor_thresholds TO gridmind_workspace USING (true);

