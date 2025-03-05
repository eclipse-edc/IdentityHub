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
    holder_id                    VARCHAR PRIMARY KEY NOT NULL, -- ID of the Holder
    participant_context_id       VARCHAR NOT NULL,             -- the DID with which this holder is identified
    did                          VARCHAR NOT NULL,             -- the DID with which this holder is identified
    holder_name                  VARCHAR,                      -- the display name of the holder
    created_date       BIGINT    NOT NULL,                     -- POSIX timestamp of the creation of the PC
    last_modified_date BIGINT                                  -- POSIX timestamp of the last modified date
);
CREATE UNIQUE INDEX IF NOT EXISTS holders_holder_id_uindex ON holders USING btree (holder_id);

