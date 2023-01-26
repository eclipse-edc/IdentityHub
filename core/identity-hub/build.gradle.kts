/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial implementation
 *
 */

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:identity-hub-spi"))
    implementation(project(":spi:identity-hub-store-spi"))
    
    implementation(libs.nimbus.jwt)
    implementation(edc.spi.transaction)

    testImplementation(edc.core.junit)
    testImplementation(testFixtures(project(":spi:identity-hub-spi")))
    testImplementation(testFixtures(project(":spi:identity-hub-store-spi")))
}
