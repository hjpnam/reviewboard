CREATE DATABASE reviewboard;
\c reviewboard;

CREATE TABLE IF NOT EXISTS company (
    id BIGSERIAL PRIMARY KEY,
    slug TEXT NOT NULL,
    name TEXT NOT NULL,
    url TEXT NOT NULL,
    location TEXT,
    country TEXT,
    industry TEXT,
    image TEXT,
    tags TEXT[]
);
