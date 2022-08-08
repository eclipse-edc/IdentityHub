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
}

val edcVersion: String by project
val edcGroup: String by project
val jupiterVersion: String by project
val nimbusVersion: String by project
val okHttpVersion: String by project
val mockitoVersion: String by project
val assertj: String by project
val faker: String by project

dependencies {
    implementation(project(":extensions:identity-hub"))
    implementation(project(":identity-hub-core:identity-hub-client"))
    implementation(project(":spi:identity-hub-spi"))
    implementation("${edcGroup}:core:${edcVersion}")
    implementation("${edcGroup}:identity-did-spi:${edcVersion}")
    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")

    testImplementation(testFixtures(project(":spi:identity-hub-spi")))
    testImplementation("com.github.javafaker:javafaker:${faker}")
    testImplementation("org.assertj:assertj-core:${assertj}")
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
    testImplementation("${edcGroup}:junit:${edcVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
}

publishing {
    publications {
        create<MavenPublication>("identity-hub-credentials-verifier") {
            artifactId = "identity-hub-credentials-verifier"
            from(components["java"])
        }
    }
}
