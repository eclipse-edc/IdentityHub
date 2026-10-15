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

CREATE TABLE IF NOT EXISTS edc_issuance_process
(
    id                          VARCHAR           NOT NULL PRIMARY KEY,
    state                       INTEGER           NOT NULL,
    state_count                 INTEGER DEFAULT 0 NOT NULL,
    state_time_stamp            BIGINT,
    created_at                  BIGINT            NOT NULL,
    updated_at                  BIGINT            NOT NULL,
    trace_context               JSON,
    error_detail                VARCHAR,
    pending                     BOOLEAN  DEFAULT FALSE,
    holder_id                   VARCHAR           NOT NULL,
    participant_context_id      VARCHAR           NOT NULL,
    holder_pid                  VARCHAR           NOT NULL,
    claims                      JSON              NOT NULL,
    credential_definitions      JSONB             NOT NULL,
    credential_formats          JSONB             NOT NULL,
    leased_by                   VARCHAR,
    leased_at                   BIGINT,
    lease_duration              INTEGER,
    version                     BIGINT  DEFAULT 0 NOT NULL
);

-- Add lease columns to tables created before the lease was moved from edc_lease to the issuance process row
ALTER TABLE edc_issuance_process ADD COLUMN IF NOT EXISTS leased_by VARCHAR;
ALTER TABLE edc_issuance_process ADD COLUMN IF NOT EXISTS leased_at BIGINT;
ALTER TABLE edc_issuance_process ADD COLUMN IF NOT EXISTS lease_duration INTEGER;
-- Add the version column to tables created before optimistic locking was introduced
ALTER TABLE edc_issuance_process ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

COMMENT ON COLUMN edc_issuance_process.leased_at IS 'posix timestamp of lease';
COMMENT ON COLUMN edc_issuance_process.lease_duration IS 'duration of lease in milliseconds';
COMMENT ON COLUMN edc_issuance_process.version IS 'version of the entity, incremented on every save, used for optimistic locking';


-- This will help to identify states that need to be transitioned without a table scan when the entries grow
CREATE INDEX IF NOT EXISTS issuance_process_state ON edc_issuance_process (state,state_time_stamp);

-- A Holder may send a credential request again, e.g. when it did not receive the response, so concurrent requests with the
-- same holderPid must not create a second issuance process
CREATE UNIQUE INDEX IF NOT EXISTS issuance_process_holder_pid ON edc_issuance_process (participant_context_id, holder_pid);

