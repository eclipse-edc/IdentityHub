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

CREATE TABLE IF NOT EXISTS edc_lease
(
    leased_by         VARCHAR NOT NULL,
    leased_at         BIGINT,
    lease_duration    INTEGER NOT NULL,
    resource_id       VARCHAR NOT NULL,
    resource_kind     VARCHAR NOT NULL,
    PRIMARY KEY(resource_id, resource_kind)
);

COMMENT ON COLUMN edc_lease.leased_at IS 'posix timestamp of lease';
COMMENT ON COLUMN edc_lease.lease_duration IS 'duration of lease in milliseconds';

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
    credential_formats          JSONB             NOT NULL
);


-- This will help to identify states that need to be transitioned without a table scan when the entries grow
CREATE INDEX IF NOT EXISTS issuance_process_state ON edc_issuance_process (state,state_time_stamp);

