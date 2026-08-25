ALTER TABLE users
    ADD COLUMN role VARCHAR(32);

UPDATE users u
SET role = COALESCE(
    (
        SELECT r.name
        FROM user_roles ur
        JOIN roles r ON r.id = ur.role_id
        WHERE ur.user_id = u.id
        ORDER BY CASE r.name
            WHEN 'ADMIN' THEN 1
            WHEN 'CHECKER' THEN 2
            WHEN 'MAKER' THEN 3
            WHEN 'CUSTOMER' THEN 4
            ELSE 5
        END
        LIMIT 1
    ),
    'CUSTOMER'
);

ALTER TABLE users
    ALTER COLUMN role SET NOT NULL,
    ADD CONSTRAINT users_role_check CHECK (role IN ('CUSTOMER', 'MAKER', 'CHECKER', 'ADMIN'));

CREATE INDEX idx_users_role ON users (role);

CREATE OR REPLACE FUNCTION bump_token_version_on_role_change()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.role IS DISTINCT FROM OLD.role THEN
        NEW.token_version := COALESCE(OLD.token_version, 0) + 1;
        NEW.updated_at := NOW();
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_role_token_version
    BEFORE UPDATE OF role ON users
    FOR EACH ROW
    EXECUTE FUNCTION bump_token_version_on_role_change();

CREATE OR REPLACE FUNCTION sync_user_roles_from_column()
RETURNS TRIGGER AS $$
BEGIN
    DELETE FROM user_roles WHERE user_id = NEW.id;
    INSERT INTO user_roles (user_id, role_id)
    SELECT NEW.id, r.id
    FROM roles r
    WHERE r.name = NEW.role;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_users_sync_role
    AFTER INSERT OR UPDATE OF role ON users
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION sync_user_roles_from_column();
