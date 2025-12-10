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
    testImplementation(project(":extensions:api:issuer-admin-api:holder-api")) // for the DTOs
    testImplementation(project(":spi:issuerservice:issuerservice-holder-spi"))
    testImplementation(project(":spi:issuerservice:issuerservice-credential-spi"))
    testImplementation(project(":spi:issuerservice:issuerservice-issuance-spi"))
    testImplementation(libs.edc.junit)
    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.mockserver.netty)

    // needed for the Participant
    testImplementation(libs.edc.transaction.local)
    testImplementation(libs.nimbus.jwt)
    testImplementation(libs.jakarta.rsApi)
    testImplementation(project(":spi:sts-spi"))
    testImplementation(testFixtures(project(":e2e-tests:identityhub-test-fixtures")))
    testImplementation(testFixtures(project(":spi:verifiable-credential-spi")))
    testImplementation(testFixtures(libs.edc.lib.oauth2.authn))

}

edcBuild {
    publish.set(false)
}
