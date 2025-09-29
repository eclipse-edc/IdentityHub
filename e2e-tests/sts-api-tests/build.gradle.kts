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
    java
}

dependencies {
    testImplementation(libs.edc.junit)
    testImplementation(project(":spi:sts-spi"))
    testImplementation(libs.edc.spi.dcp)
    testImplementation(libs.edc.oauth2.client)

    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
    testImplementation(libs.junit.jupiter.api)

    testImplementation(testFixtures(project(":spi:sts-spi")))
    testImplementation(testFixtures(libs.edc.sql.test.fixtures))
    testImplementation(libs.edc.transaction.local)
    testImplementation(libs.nimbus.jwt)
    testImplementation(libs.bouncyCastle.bcpkixJdk18on)

    testImplementation(testFixtures(libs.edc.lib.http))
}

edcBuild {
    publish.set(false)
}
