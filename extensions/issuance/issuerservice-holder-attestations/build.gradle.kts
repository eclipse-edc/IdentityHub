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
    api(libs.edc.spi.core)
    api(libs.edc.spi.transaction)
    implementation(libs.edc.spi.validator)
    implementation(libs.edc.lib.sql)

    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.lib.json)
    testImplementation(testFixtures(libs.edc.sql.test.fixtures))
}
