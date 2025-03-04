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

create table if not exists membership_attestation
(
    membership_type       integer   default 0,
    holder_id             varchar                             not null,
    membership_start_date timestamp default now()             not null,
    id                    varchar   default gen_random_uuid() not null
);



INSERT INTO membership_attestation (membership_type, holder_id) VALUES (0, 'holder-1')