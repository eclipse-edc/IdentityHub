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
    api(libs.edc.spi.web)
    api(libs.edc.sts.spi)
    implementation(libs.jakarta.rsApi)

    testImplementation(libs.edc.junit)
    testImplementation(testFixtures(libs.edc.core.jersey))
    testImplementation(testFixtures(libs.edc.sts.spi))
    testImplementation(libs.restAssured)
}

edcBuild {
    swagger {
        apiGroup.set("sts-api")
    }
}
