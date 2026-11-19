ALTER TABLE transformers
    ADD COLUMN asset_type VARCHAR(20) NOT NULL DEFAULT 'TEST';

UPDATE transformers
SET asset_type = 'HARDWARE'
WHERE code = 'T-N-001';

CREATE INDEX idx_transformers_asset_type ON transformers(asset_type);
