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
 *       Microsoft Corporation - initial API and implementation
 *
 */

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(project(":spi:identity-hub-client-spi"))
    api(edc.spi.core)
    implementation(edc.ext.http)
    implementation(libs.okhttp)
    implementation(libs.jackson.databind)
    implementation(edc.spi.core)
    implementation(libs.nimbus.jwt)

    testImplementation(project(":core:identity-hub"))
    testImplementation(project(":extensions:identity-hub-api"))
    testImplementation(testFixtures(project(":spi:identity-hub-spi")))
    testImplementation(edc.core.junit)
}

publishing {
    publications {
        create<MavenPublication>("identity-hub-client") {
            artifactId = "identity-hub-client"
            from(components["java"])
        }
    }
}
