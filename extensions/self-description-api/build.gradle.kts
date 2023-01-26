/*
 *  Copyright (c) 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

plugins {
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
    `maven-publish`
}

dependencies {
    implementation(project(":extensions:identity-hub-api"))

    implementation(edc.ext.http)
    testImplementation(edc.core.junit)
    testImplementation(libs.restAssured)
}
