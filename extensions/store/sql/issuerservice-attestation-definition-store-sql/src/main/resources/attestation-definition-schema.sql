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
    created_date                 BIGINT     NOT NULL        ,
    last_modified_date           BIGINT     NOT NULL        ,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS attestation_definition_ix
    ON attestation_definitions (id);


