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
    api(project(":spi:issuerservice:issuerservice-participant-spi"))
    implementation(project(":extensions:api:issuer-admin-api:issuer-admin-api-configuration"))
    implementation(libs.edc.spi.validator)
    implementation(libs.edc.spi.web)
    implementation(libs.edc.lib.util)
    implementation(libs.edc.lib.jerseyproviders)
    implementation(libs.jakarta.rsApi)
    implementation(libs.jakarta.annotation)


    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.jsonld)
    testImplementation(testFixtures(libs.edc.core.jersey))
    testImplementation(libs.nimbus.jwt)
    testImplementation(libs.restAssured)
    testImplementation(libs.tink)
}

edcBuild {
    swagger {
        apiGroup.set("issuer-admin-api")
    }
}
