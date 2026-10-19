/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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
CREATE TABLE IF NOT EXISTS keypair_resource
(
    id                     VARCHAR PRIMARY KEY NOT NULL,               -- primary key
    participant_context_id VARCHAR,                                    -- ID of the owning ParticipantContext. this is a loose business key, not a FK!
    timestamp              BIGINT              NOT NULL,               -- creation timestamp
    key_id                 VARCHAR             NOT NULL,               -- name/key-id of this key pair. for use in JWTs etc.
    group_name             VARCHAR,
    is_default_pair        BOOLEAN                      DEFAULT FALSE, -- whether this keypair is the default one for a participant context
    use_duration           BIGINT,                                     -- maximum time this keypair can be active before it gets rotated
    rotation_duration      BIGINT,                                     -- duration during which this keypair is in a transitional state (rotated, not yet deactivated)
    serialized_public_key  VARCHAR             NOT NULL,               -- serialized public key (PEM, JWK,...)
    private_key_alias      VARCHAR             NOT NULL,               -- alias under which the private key is stored in the HSM/Vault
    private_key_managed    BOOLEAN             NOT NULL DEFAULT FALSE, -- whether the private key was stored in the vault by IdentityHub
    state                  INT                 NOT NULL DEFAULT 100,   -- KeyPairState
    key_context            VARCHAR,                                    -- the key context, will end up in the VerificationMethod of the DID Document
    usage                  VARCHAR             NOT NULL                -- array of usages for this key pair
);

CREATE INDEX IF NOT EXISTS keypair_resource_participant_context_id ON keypair_resource (participant_context_id);
-- a participant context has at most one key pair per key ID that is in use (state CREATED or ACTIVATED). A rotated key
-- pair is not included, because a rotated Transit key keeps its key ID. A table that already holds such duplicates is
-- left as it is, with a warning, until they are rotated, revoked or deleted
DO
$$
    BEGIN
        CREATE UNIQUE INDEX IF NOT EXISTS keypair_resource_participant_key_id ON keypair_resource (participant_context_id, key_id) WHERE state IN (100, 200);
    EXCEPTION
        WHEN unique_violation THEN
            RAISE WARNING 'Index keypair_resource_participant_key_id not created: a participant context has several key pairs in use with the same key ID';
    END
$$;
