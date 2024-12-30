CREATE DATABASE reviewboard;
\c reviewboard;

CREATE TABLE IF NOT EXISTS company (
    id BIGSERIAL PRIMARY KEY,
    slug TEXT UNIQUE NOT NULL,
    name TEXT UNIQUE NOT NULL,
    url TEXT UNIQUE NOT NULL,
    location TEXT,
    country TEXT,
    industry TEXT,
    image TEXT,
    tags TEXT[]
);

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

CREATE TABLE IF NOT EXISTS usr (
    id BIGSERIAL PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    hashed_password TEXT NOT NULL
);
