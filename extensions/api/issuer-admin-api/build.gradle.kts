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

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":extensions:api:issuer-admin-api:administration-api"))
    api(project(":extensions:api:issuer-admin-api:attestation-api"))
    api(project(":extensions:api:issuer-admin-api:credentials-api"))
    api(project(":extensions:api:issuer-admin-api:participant-api"))
}
