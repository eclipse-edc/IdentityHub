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
CREATE TABLE IF NOT EXISTS attestation_definitions
(
    id                           VARCHAR    NOT NULL        ,
    participant_context_id       VARCHAR    NOT NULL        ,
    attestation_type             VARCHAR    NOT NULL        ,
    configuration                JSON       DEFAULT '{}'    ,
    private_properties           JSON       DEFAULT '{}'    ,
    created_date                 BIGINT     NOT NULL        ,
    last_modified_date           BIGINT     NOT NULL        ,
    CONSTRAINT attestation_definitions_participant_id_pk PRIMARY KEY (participant_context_id, id)
);
-- migration of tables in which the attestation definition ID was unique across all participant contexts. The unique
-- index takes the place of the composite primary key, and is a no-op if the table already has it
ALTER TABLE attestation_definitions DROP CONSTRAINT IF EXISTS attestation_definitions_pkey;
DROP INDEX IF EXISTS attestation_definition_ix;
CREATE UNIQUE INDEX IF NOT EXISTS attestation_definitions_participant_id_pk ON attestation_definitions (participant_context_id, id);


