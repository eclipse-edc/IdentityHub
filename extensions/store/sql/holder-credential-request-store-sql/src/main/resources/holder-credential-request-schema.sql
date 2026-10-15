/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

-- only intended for and tested with Postgres!
CREATE TABLE IF NOT EXISTS edc_holder_credentialrequest
(
    id                     VARCHAR           NOT NULL PRIMARY KEY, -- this is also the holderPid
    state                  INTEGER           NOT NULL,
    state_count            INTEGER DEFAULT 0 NOT NULL,
    state_time_stamp       BIGINT,
    created_at             BIGINT            NOT NULL,
    updated_at             BIGINT            NOT NULL,
    trace_context          JSON,
    error_detail           VARCHAR,
    pending                BOOLEAN DEFAULT FALSE,
    participant_context_id VARCHAR           NOT NULL,
    issuer_did             VARCHAR           NOT NULL,
    ids_and_formats        JSON              NOT NULL,
    issuer_pid             VARCHAR,
    leased_by              VARCHAR,
    leased_at              BIGINT,
    lease_duration         INTEGER,
    version                BIGINT  DEFAULT 0 NOT NULL
);

-- Add lease columns to tables created before the lease was moved from edc_lease to the credential request row
ALTER TABLE edc_holder_credentialrequest ADD COLUMN IF NOT EXISTS leased_by VARCHAR;
ALTER TABLE edc_holder_credentialrequest ADD COLUMN IF NOT EXISTS leased_at BIGINT;
ALTER TABLE edc_holder_credentialrequest ADD COLUMN IF NOT EXISTS lease_duration INTEGER;
-- Add the version column to tables created before optimistic locking was introduced
ALTER TABLE edc_holder_credentialrequest ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

COMMENT ON COLUMN edc_holder_credentialrequest.leased_at IS 'posix timestamp of lease';
COMMENT ON COLUMN edc_holder_credentialrequest.lease_duration IS 'duration of lease in milliseconds';
COMMENT ON COLUMN edc_holder_credentialrequest.version IS 'version of the entity, incremented on every save, used for optimistic locking';


-- This will help to identify states that need to be transitioned without a table scan when the entries grow
CREATE INDEX IF NOT EXISTS holder_credentialrequest_state ON edc_holder_credentialrequest (state, state_time_stamp);

