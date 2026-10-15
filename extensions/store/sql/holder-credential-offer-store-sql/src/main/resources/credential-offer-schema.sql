/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

-- only intended for and tested with Postgres!
CREATE TABLE IF NOT EXISTS edc_credential_offers
(
    id                     VARCHAR NOT NULL PRIMARY KEY, -- this is also the holderPid
    state                  INTEGER NOT NULL,
    state_count            INTEGER          DEFAULT 0 NOT NULL,
    state_time_stamp       BIGINT,
    created_at             BIGINT  NOT NULL,
    updated_at             BIGINT  NOT NULL,
    trace_context          JSON,
    error_detail           VARCHAR,
    participant_context_id VARCHAR NOT NULL,
    issuer_did             VARCHAR NOT NULL,
    credentials            JSON    NOT NULL DEFAULT '{}',
    leased_by              VARCHAR,
    leased_at              BIGINT,
    lease_duration         INTEGER,
    version                BIGINT  NOT NULL DEFAULT 0
);

-- Add lease columns to tables created before the lease was moved from edc_lease to the credential offer row
ALTER TABLE edc_credential_offers ADD COLUMN IF NOT EXISTS leased_by VARCHAR;
ALTER TABLE edc_credential_offers ADD COLUMN IF NOT EXISTS leased_at BIGINT;
ALTER TABLE edc_credential_offers ADD COLUMN IF NOT EXISTS lease_duration INTEGER;
-- Add the version column to tables created before optimistic locking was introduced
ALTER TABLE edc_credential_offers ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

COMMENT ON COLUMN edc_credential_offers.leased_at IS 'posix timestamp of lease';
COMMENT ON COLUMN edc_credential_offers.lease_duration IS 'duration of lease in milliseconds';
COMMENT ON COLUMN edc_credential_offers.version IS 'version of the entity, incremented on every save, used for optimistic locking';


-- This will help to identify states that need to be transitioned without a table scan when the entries grow
CREATE INDEX IF NOT EXISTS credential_offer_state ON edc_credential_offers (state, state_time_stamp);

