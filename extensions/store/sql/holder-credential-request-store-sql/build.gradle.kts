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
    api(project(":spi:holder-credential-request-spi"))
    implementation(libs.edc.lib.sql)
    implementation(libs.edc.sql.lease)
    implementation(libs.edc.sql.bootstrapper)
    implementation(libs.edc.spi.transaction.datasource)

    testImplementation(testFixtures(project(":spi:holder-credential-request-spi")))
    testImplementation(testFixtures(libs.edc.sql.test.fixtures))
    testImplementation(libs.edc.junit)
}
