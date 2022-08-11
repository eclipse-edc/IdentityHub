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
    id("io.swagger.core.v3.swagger-gradle-plugin")
    `maven-publish`
}
val assertj: String by project
val edcVersion: String by project
val edcGroup: String by project
val jupiterVersion: String by project
val restAssured: String by project
val faker: String by project
val nimbusVersion: String by project
val mockitoVersion: String by project

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(project(":identity-hub-core:identity-hub-model"))
    implementation(project(":spi:identity-hub-store-spi"))
    implementation("${edcGroup}:http:${edcVersion}")
    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")

    testImplementation("${edcGroup}:common-util:${edcVersion}:test-fixtures")
    testImplementation("${edcGroup}:junit:${edcVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
    testImplementation("org.assertj:assertj-core:${assertj}")
    testImplementation("io.rest-assured:rest-assured:${restAssured}")
    testImplementation("com.github.javafaker:javafaker:${faker}")
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
    testImplementation(testFixtures(project(":spi:identity-hub-spi")))
}

publishing {
    publications {
        create<MavenPublication>("identity-hub") {
            artifactId = "identity-hub"
            from(components["java"])
        }
    }
}