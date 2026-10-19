/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

-- only intended for and tested with Postgres!
CREATE TABLE IF NOT EXISTS credential_resource
(
    id                     VARCHAR             NOT NULL, -- unique per participant context only
    create_timestamp       BIGINT              NOT NULL, -- POSIX timestamp of the creation of the VC
    issuer_id              VARCHAR             NOT NULL,
    holder_id              VARCHAR             NOT NULL,
    vc_state               INTEGER             NOT NULL,
    metadata               JSON DEFAULT '{}',
    issuance_policy        JSON,
    reissuance_policy      JSON,
    raw_vc                 VARCHAR,                      -- Representation of the VC exactly as it was received by the issuer. Can be JWT or JSON(-LD)
    vc_format              INTEGER             NOT NULL, -- 0 = JSON-LD, 1 = JWT
    verifiable_credential  JSON                NOT NULL, -- JSON-representation of the verifiable credential
    participant_context_id VARCHAR             NOT NULL, -- ID of the ParticipantContext that owns this credentials
    usage                  VARCHAR             NOT NULL, -- 'holder', 'statuslist' or 'issuancetracking'
    CONSTRAINT credential_resource_participant_id_pk PRIMARY KEY (participant_context_id, id)
);
-- migration of tables in which the credential ID was unique across all participant contexts. The unique index takes
-- the place of the composite primary key, and is a no-op if the table already has it
ALTER TABLE credential_resource DROP CONSTRAINT IF EXISTS credential_resource_pkey;
DROP INDEX IF EXISTS credential_resource_credential_id_uindex;
CREATE UNIQUE INDEX IF NOT EXISTS credential_resource_participant_id_pk ON credential_resource (participant_context_id, id);
COMMENT ON COLUMN credential_resource.id IS 'ID of the VC, duplicated here for indexing purposes';
COMMENT ON COLUMN credential_resource.raw_vc IS 'Representation of the VC exactly as it was received by the issuer. Can be JWT or JSON(-LD) ';
COMMENT ON COLUMN credential_resource.vc_format IS '0 = JSON-LD, 1 = JWT';
COMMENT ON COLUMN credential_resource.verifiable_credential IS 'JSON-representation of the VerifiableCredential';

CREATE INDEX IF NOT EXISTS credential_resource_participant_usage_state ON credential_resource (participant_context_id, usage, vc_state);
