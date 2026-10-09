CREATE TABLE user_accounts (
    id VARCHAR(36) PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE auth_bootstrap (
    id INTEGER PRIMARY KEY,
    bootstrap_complete BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO auth_bootstrap (id, bootstrap_complete) VALUES (1, FALSE);
