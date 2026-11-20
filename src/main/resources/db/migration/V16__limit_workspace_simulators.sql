CREATE FUNCTION limit_workspace_test_transformers() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    account_id text := current_setting('gridmind.account_id', true);
BEGIN
    IF current_user <> 'gridmind_workspace' OR NEW.asset_type <> 'TEST' OR NEW.archived_at IS NOT NULL THEN
        RETURN NEW;
    END IF;

    PERFORM pg_advisory_xact_lock(hashtextextended(account_id, 1001));
    IF (SELECT count(*) FROM transformers t
        WHERE t.asset_type = 'TEST' AND t.archived_at IS NULL AND t.id <> NEW.id) >= 10 THEN
        RAISE EXCEPTION 'Tu cuenta puede tener hasta 10 transformadores simulados activos.' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER workspace_test_transformer_limit
BEFORE INSERT OR UPDATE OF archived_at, asset_type, substation_id ON transformers
FOR EACH ROW EXECUTE FUNCTION limit_workspace_test_transformers();

