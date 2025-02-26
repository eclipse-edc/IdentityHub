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

plugins {
    `java-library`
}

dependencies {
    api(project(":extensions:api:identity-api:api-configuration"))
    api(project(":extensions:api:identity-api:did-api"))
    api(project(":extensions:api:identity-api:keypair-api"))
    api(project(":extensions:api:identity-api:participant-context-api"))
    api(project(":extensions:api:identity-api:validators"))
    api(project(":extensions:api:identity-api:verifiable-credentials-api"))
}
