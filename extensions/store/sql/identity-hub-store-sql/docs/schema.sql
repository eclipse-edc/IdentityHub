CREATE TABLE IF NOT EXISTS edc_identityhub
(
    id                   VARCHAR NOT NULL PRIMARY KEY,
    payload              VARCHAR NOT NULL UNIQUE,
    created_at           BIGINT NOT NULL
);