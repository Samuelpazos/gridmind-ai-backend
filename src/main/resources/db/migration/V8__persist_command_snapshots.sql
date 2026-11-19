CREATE TABLE command_snapshots (
    id VARCHAR(36) PRIMARY KEY,
    command_log_id VARCHAR(36) NOT NULL UNIQUE REFERENCES command_logs(id) ON DELETE CASCADE,
    memento_state TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    consumed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_command_snapshots_available
    ON command_snapshots(consumed_at, created_at DESC);

INSERT INTO command_snapshots (id, command_log_id, memento_state, created_at)
SELECT gen_random_uuid()::text, id, memento_state, executed_at
FROM command_logs
WHERE undoable = TRUE AND memento_state IS NOT NULL
ON CONFLICT (command_log_id) DO NOTHING;
