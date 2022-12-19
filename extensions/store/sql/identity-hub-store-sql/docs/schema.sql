CREATE TABLE IF NOT EXISTS edc_identityhub
(
    id                   VARCHAR NOT NULL PRIMARY KEY,
    payload              VARCHAR NOT NULL UNIQUE,
    payloadFormat        VARCHAR NOT NULL,
    created_at           BIGINT NOT NULL
);
