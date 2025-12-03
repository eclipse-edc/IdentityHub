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
    implementation(project(":dist:bom:issuerservice-base-bom"))
    implementation(project(":extensions:api:identityhub-api-authentication"))
    implementation(project(":extensions:api:identityhub-api-authorization"))
    implementation(project(":extensions:api:issuer-admin-api:issuer-admin-api-authentication"))
    // needed for interaction with the embedded STS
    implementation(project(":extensions:sts:sts-core"))
    implementation(project(":extensions:sts:sts-account-service-local"))
    implementation(project(":extensions:sts:sts-api"))
}
