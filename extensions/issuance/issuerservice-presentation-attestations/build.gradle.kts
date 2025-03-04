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
    `java-test-fixtures`
    `maven-publish`
}

dependencies {
    api(project(":spi:issuerservice:issuerservice-issuance-spi"))
    api(libs.edc.spi.core)

    implementation(libs.edc.spi.validator)

    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.lib.json)

    testFixturesImplementation(libs.edc.spi.identity.did)
    testFixturesImplementation(libs.junit.jupiter.api)
    testFixturesImplementation(libs.edc.junit)
    testFixturesImplementation(libs.assertj)
}
