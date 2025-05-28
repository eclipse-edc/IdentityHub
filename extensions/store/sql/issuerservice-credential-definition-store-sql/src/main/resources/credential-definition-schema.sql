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
CREATE TABLE IF NOT EXISTS credential_definitions
(
    id                     VARCHAR NOT NULL,
    participant_context_id VARCHAR NOT NULL,
    credential_type        VARCHAR NOT NULL UNIQUE,
    attestations           JSON    NOT NULL DEFAULT '[]',
    rules                  JSON    NOT NULL DEFAULT '[]',
    mappings               JSON    NOT NULL DEFAULT '[]',
    json_schema            JSON,
    json_schema_url        VARCHAR,
    validity               BIGINT  NOT NULL,
    format                 VARCHAR NOT NULL,
    created_date           BIGINT  NOT NULL, -- POSIX timestamp of the creation of the PC
    last_modified_date     BIGINT,           -- POSIX timestamp of the last modified date
    PRIMARY KEY (id)
);


