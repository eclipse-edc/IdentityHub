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
 *       Microsoft Corporation - initial API and implementation
 *
 */

plugins {
    `java-library`
    `maven-publish`
}

val jacksonVersion: String by project
val okHttpVersion: String by project
val edcVersion: String by project
val edcGroup: String by project
val jupiterVersion: String by project
val faker: String by project
val assertj: String by project
val nimbusVersion: String by project
val mockitoVersion: String by project

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(project(":identity-hub-core:identity-hub-model"))
    api("${edcGroup}:core-spi:${edcVersion}")
    implementation("${edcGroup}:http:${edcVersion}")
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("${edcGroup}:core-spi:${edcVersion}")
    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")

    testImplementation(project(":extensions:identity-hub"))
    testImplementation(testFixtures(project(":spi:identity-hub-spi")))
    testImplementation("${edcGroup}:common-util:${edcVersion}:test-fixtures")
    testImplementation("${edcGroup}:junit:${edcVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
    testImplementation("org.assertj:assertj-core:${assertj}")
    testImplementation("com.github.javafaker:javafaker:${faker}")
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
}

publishing {
    publications {
        create<MavenPublication>("identity-hub-client") {
            artifactId = "identity-hub-client"
            from(components["java"])
        }
    }
}