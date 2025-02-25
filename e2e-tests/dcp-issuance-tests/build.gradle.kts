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

    testImplementation(project(":spi:issuerservice:issuerservice-participant-spi"))
    testImplementation(project(":spi:issuerservice:issuerservice-credential-spi"))
    testImplementation(project(":spi:issuerservice:issuerservice-issuance-spi"))
    testImplementation(project(":spi:participant-context-spi"))
    testImplementation(project(":spi:sts-spi"))

    testImplementation(libs.edc.junit)
    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
    testImplementation(testFixtures(project(":e2e-tests:fixtures")))
    testImplementation(libs.mockserver.netty)
}

edcBuild {
    publish.set(false)
}