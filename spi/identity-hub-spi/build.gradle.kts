/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial implementation
 *
 */

plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
}

val swagger: String by project

dependencies {

    implementation(libs.jackson.databind)
    implementation(libs.nimbus.jwt)
    implementation(edc.spi.identity.did)

    implementation(libs.swagger.jaxrs) {
        exclude(group = "com.fasterxml.jackson.jaxrs", module = "jackson-jaxrs-json-provider")
    }

    testFixturesImplementation(libs.nimbus.jwt)
    testFixturesImplementation(edc.spi.identity.did)
}
