CREATE TABLE IF NOT EXISTS review (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    management SMALLINT NOT NULL,
    culture SMALLINT NOT NULL,
    salaries SMALLINT NOT NULL,
    benefits SMALLINT NOT NULL,
    would_recommend SMALLINT NOT NULL,
    review TEXT NOT NULL,
    created TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated TIMESTAMPTZ NOT NULL DEFAULT now()
);
