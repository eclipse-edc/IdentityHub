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
    credentials            JSON    NOT NULL DEFAULT '{}'
);


-- This will help to identify states that need to be transitioned without a table scan when the entries grow
CREATE INDEX IF NOT EXISTS credential_offer_state ON edc_credential_offers (state, state_time_stamp);

