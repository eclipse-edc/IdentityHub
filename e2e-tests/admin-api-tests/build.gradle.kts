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
    testImplementation(project(":core:identity-hub-participants"))
    testImplementation(project(":extensions:api:issuer-admin-api:participant-api")) // for the DTOs
    testImplementation(project(":spi:issuerservice:issuerservice-participant-spi"))
    testImplementation(libs.edc.junit)
    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgres)

    // needed for the Participant
    testImplementation(testFixtures(libs.edc.testfixtures.managementapi))
    testImplementation(testFixtures(libs.edc.sql.test.fixtures))
    testImplementation(libs.edc.transaction.local)
    testImplementation(libs.nimbus.jwt)
    testImplementation(libs.jakarta.rsApi)
    testImplementation(libs.edc.sts.spi)
    testImplementation(testFixtures(project(":e2e-tests:fixtures")))

    testCompileOnly(project(":dist:bom:identityhub-with-sts-bom"))
    testCompileOnly(project(":dist:bom:identityhub-feature-sql-bom"))
}

edcBuild {
    publish.set(false)
}
