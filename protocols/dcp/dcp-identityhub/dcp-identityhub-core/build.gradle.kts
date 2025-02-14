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
    api(project(":spi:identity-hub-spi"))
    api(project(":protocols:dcp:dcp-spi"))
    api(libs.edc.spi.identity.did)
    implementation(libs.edc.vc.jwt)
    implementation(libs.edc.lib.token)
    testImplementation(libs.edc.junit)
    testImplementation(testFixtures(project(":spi:verifiable-credential-spi")))
}

