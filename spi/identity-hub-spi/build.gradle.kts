/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
    `java-test-fixtures`
    `maven-publish`
}

val swagger: String by project

dependencies {

    api(libs.edc.spi.identitytrust)
    api(libs.edc.spi.vc)
    api(libs.edc.spi.web)
    implementation(libs.jackson.databind)
    implementation(libs.nimbus.jwt)
    implementation(libs.edc.spi.identity.did)

    implementation(libs.swagger.jaxrs) {
        exclude(group = "com.fasterxml.jackson.jaxrs", module = "jackson-jaxrs-json-provider")
    }

    testFixturesImplementation(libs.nimbus.jwt)
    testFixturesImplementation(libs.edc.spi.identity.did)
}
