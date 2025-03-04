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
CREATE TABLE IF NOT EXISTS participants
(
    participant_id     VARCHAR PRIMARY KEY NOT NULL, -- ID of the Participant
    did                VARCHAR            NOT NULL, -- the DID with which this participant is identified
    participant_name   VARCHAR,                      -- the display name of the participant
    created_date       BIGINT              NOT NULL, -- POSIX timestamp of the creation of the PC
    last_modified_date BIGINT,                        -- POSIX timestamp of the last modified date
    attestations       JSON       DEFAULT '[]'       -- enabled attestations for this participant
);
CREATE UNIQUE INDEX IF NOT EXISTS participants_participant_id_uindex ON participants USING btree (participant_id);

