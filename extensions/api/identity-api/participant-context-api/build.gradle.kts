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
    `maven-publish`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    api(libs.edc.spi.core)
    api(project(":spi:identity-hub-spi"))
    implementation(project(":extensions:api:identity-api:api-configuration"))
    implementation(project(":extensions:api:identity-api:validators:participant-context-validators"))
    implementation(libs.edc.spi.validator)
    implementation(libs.edc.spi.web)
    implementation(libs.edc.lib.util)
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
        apiGroup.set("identity-api")
    }
}
