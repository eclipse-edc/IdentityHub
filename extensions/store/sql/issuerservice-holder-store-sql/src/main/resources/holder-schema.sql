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
CREATE TABLE IF NOT EXISTS holders
(
    holder_id              VARCHAR             NOT NULL,              -- ID of the Holder, unique per participant context only
    participant_context_id VARCHAR             NOT NULL,              -- the DID with which this holder is identified
    did                    VARCHAR             NOT NULL,              -- the DID with which this holder is identified
    holder_name            VARCHAR,                                   -- the display name of the holder
    anonymous              BOOLEAN             NOT NULL DEFAULT FALSE,-- indicates whether the holder was created during a credential request
    created_date           BIGINT              NOT NULL,              -- POSIX timestamp of the creation of the PC
    last_modified_date     BIGINT,                                    -- POSIX timestamp of the last modified date
    properties             JSON DEFAULT '{}',                         -- JSON serialization of the properties
    CONSTRAINT holders_participant_id_pk PRIMARY KEY (participant_context_id, holder_id)
);
-- migration of tables in which the holder ID was unique across all participant contexts. The unique index takes the
-- place of the composite primary key, and is a no-op if the table already has it
ALTER TABLE holders DROP CONSTRAINT IF EXISTS holders_pkey;
DROP INDEX IF EXISTS holders_holder_id_uindex;
CREATE UNIQUE INDEX IF NOT EXISTS holders_participant_id_pk ON holders (participant_context_id, holder_id);
-- a participant context has at most one holder per DID. A table that already holds such duplicates is left as it is,
-- with a warning, until they are deleted
DO
$$
    BEGIN
        CREATE UNIQUE INDEX IF NOT EXISTS holders_participant_did ON holders (participant_context_id, did);
    EXCEPTION
        WHEN unique_violation THEN
            RAISE WARNING 'Index holders_participant_did not created: a participant context has several holders with the same DID';
    END
$$;

