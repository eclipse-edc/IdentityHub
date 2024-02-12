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
    api(project(":spi:identity-hub-did-spi"))

    implementation(libs.edc.spi.web)
    implementation(libs.edc.core.jerseyproviders)
    implementation(libs.jakarta.rsApi)

    testImplementation(libs.edc.junit)
    testRuntimeOnly(libs.jersey.common) // needs the RuntimeDelegate
}
