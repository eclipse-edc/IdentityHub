/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

plugins {
    `java-library`
    `maven-publish`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(project(":spi:verifiable-credential-spi"))
    api(project(":protocols:dcp:dcp-spi"))
    api(libs.edc.spi.jsonld)
    api(libs.edc.spi.jwt)
    api(libs.edc.spi.core)
    implementation(project(":protocols:dcp:dcp-identityhub:credentials-api-configuration"))
    implementation(libs.edc.spi.validator)
    implementation(libs.edc.spi.web)
    implementation(libs.edc.spi.dcp)
    implementation(libs.edc.lib.jerseyproviders)
    implementation(libs.edc.lib.transform)
    implementation(libs.edc.dcp.transform)
    implementation(libs.jakarta.rsApi)
    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.jsonld)
    testImplementation(testFixtures(libs.edc.core.jersey))
    testImplementation(testFixtures(project(":spi:verifiable-credential-spi")))
    testImplementation(libs.nimbus.jwt)
}

edcBuild {
    swagger {
        apiGroup.set("credentials-api")
    }
}
