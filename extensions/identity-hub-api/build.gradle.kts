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
    id("io.swagger.core.v3.swagger-gradle-plugin")
    `maven-publish`
}

dependencies {
    api(project(":spi:identity-hub-spi"))
    implementation(project(":spi:identity-hub-store-spi"))
    implementation(project(":core:identity-hub"))
    implementation(edc.ext.http)
    implementation(edc.spi.transaction)


    testImplementation(edc.core.junit)
    testImplementation(libs.nimbus.jwt)
    testImplementation(libs.restAssured)
    testImplementation(project(":spi:identity-hub-spi"))
    testImplementation(project(":extensions:credentials:identity-hub-credentials-jwt"))

    testImplementation(testFixtures(project(":spi:identity-hub-spi")))
    testImplementation(testFixtures(project(":spi:identity-hub-store-spi")))
}


publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}

