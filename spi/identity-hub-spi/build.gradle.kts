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
    `java-test-fixtures`
    `maven-publish`
}

val jetBrainsAnnotationsVersion: String by project
val jacksonVersion: String by project
val nimbusVersion: String by project
val faker: String by project
val edcGroup: String by project
val edcVersion: String by project
val jupiterVersion: String by project
val assertj: String by project
val mockitoVersion: String by project

dependencies {
    api("org.jetbrains:annotations:${jetBrainsAnnotationsVersion}")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
    implementation("${edcGroup}:identity-did-spi:${edcVersion}")
    implementation("${edcGroup}:identity-did-crypto:${edcVersion}")

    testFixturesImplementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
    testFixturesImplementation("com.github.javafaker:javafaker:${faker}")
    testFixturesImplementation("${edcGroup}:identity-did-spi:${edcVersion}")
    testFixturesImplementation("${edcGroup}:identity-did-crypto:${edcVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
    testImplementation("org.assertj:assertj-core:${assertj}")
    testImplementation("com.github.javafaker:javafaker:${faker}")
}

publishing {
    publications {
        create<MavenPublication>("identity-hub-spi") {
            artifactId = "identity-hub-spi"
            from(components["java"])
        }
    }
}