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
    `maven-publish`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    api(libs.edc.spi.core)
    api(project(":spi:issuerservice:issuerservice-credential-spi"))
    api(project(":spi:issuerservice:issuerservice-holder-spi"))
    implementation(project(":extensions:api:issuer-admin-api:issuer-admin-api-configuration"))
    implementation(project(":protocols:dcp:dcp-transform-lib"))
    implementation(libs.edc.spi.web)
    implementation(libs.jakarta.rsApi)


    testImplementation(libs.edc.junit)
    testImplementation(testFixtures(libs.edc.core.jersey))
    testImplementation(libs.restAssured)
}

edcBuild {
    swagger {
        apiGroup.set("issuer-admin-api")
    }
}
