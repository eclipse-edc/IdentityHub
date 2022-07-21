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

dependencies {
    api("org.jetbrains:annotations:${jetBrainsAnnotationsVersion}")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

    testFixturesImplementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
    testFixturesImplementation("com.github.javafaker:javafaker:${faker}")
    testFixturesImplementation("${edcGroup}:identity-did-spi:${edcVersion}")
    testFixturesImplementation("${edcGroup}:identity-did-crypto:${edcVersion}")
    testFixturesImplementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
}

publishing {
    publications {
        create<MavenPublication>("identity-hub-spi") {
            artifactId = "identity-hub-spi"
            from(components["java"])
        }
    }
}