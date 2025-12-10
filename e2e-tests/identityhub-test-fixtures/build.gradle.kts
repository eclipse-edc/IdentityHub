/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

plugins {
    `java-library`
    `java-test-fixtures`
}


dependencies {
    testFixturesApi(project(":spi:identity-hub-spi"))
    testFixturesApi(project(":spi:issuerservice:issuerservice-issuance-spi"))
    testFixturesApi(project(":spi:issuerservice:issuerservice-holder-spi"))
    testFixturesApi(project(":spi:keypair-spi"))
    testFixturesApi(project(":spi:did-spi"))
    testFixturesApi(project(":spi:holder-credential-request-spi"))
    testFixturesApi(testFixtures(project(":spi:verifiable-credential-spi")))
    testFixturesApi(libs.junit.jupiter.api)
    testFixturesApi(libs.edc.junit)
    testFixturesApi(libs.edc.spi.did)
    testFixturesApi(libs.restAssured)
    testFixturesApi(libs.awaitility)
    testFixturesApi(libs.edc.transaction.local)
    testFixturesApi(libs.jakarta.rsApi)
    testFixturesApi(libs.testcontainers.junit)
    testFixturesApi(libs.testcontainers.postgres)
    testFixturesApi(testFixtures(libs.edc.sql.test.fixtures))
}
