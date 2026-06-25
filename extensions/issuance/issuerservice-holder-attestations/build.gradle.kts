/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
}

dependencies {
    api(project(":spi:issuerservice:issuerservice-issuance-spi"))
    implementation(libs.edc.spi.core)
    implementation(libs.edc.lib.core)

    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.lib.jsonld)
    testImplementation(testFixtures(libs.edc.sql.test.fixtures))
}
