CREATE TABLE IF NOT EXISTS recovery_token (
    email TEXT PRIMARY KEY,
    token TEXT NOT NULL,
    expiration BIGINT NOT NULL
);
