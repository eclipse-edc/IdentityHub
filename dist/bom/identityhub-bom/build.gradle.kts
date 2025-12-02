/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":dist:bom:identityhub-base-bom"))
    implementation(project(":extensions:api:identityhub-api-authentication"))
    implementation(project(":extensions:api:identityhub-api-authorization"))
    implementation(project(":extensions:sts:sts-account-service-local"))
    implementation(project(":extensions:sts:sts-core"))
    implementation(project(":extensions:sts:sts-api"))
    runtimeOnly(libs.edc.core.api)
}
