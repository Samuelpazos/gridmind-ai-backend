ALTER TABLE transformers
    ADD COLUMN archived_at TIMESTAMPTZ;

CREATE INDEX idx_transformers_archived_at
    ON transformers (archived_at);
