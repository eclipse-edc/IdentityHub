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
}

val edcVersion: String by project
val edcGroup: String by project
val jacksonVersion: String by project
val jupiterVersion: String by project
val assertj: String by project
val mockitoVersion: String by project
val okHttpVersion: String by project
val nimbusVersion: String by project
val bouncycastleVersion: String by project
val picoCliVersion: String by project

dependencies {
    api(project(":spi:identity-hub-spi"))
    api("${edcGroup}:identity-did-spi:${edcVersion}")
    testImplementation(project(":launcher"))
    testImplementation(project(":client-cli"))

    testImplementation("${edcGroup}:junit:${edcVersion}")
    testImplementation("info.picocli:picocli:${picoCliVersion}")
    testImplementation("info.picocli:picocli-codegen:${picoCliVersion}")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
    testImplementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    testImplementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
    testImplementation("org.bouncycastle:bcpkix-jdk15on:${bouncycastleVersion}")
    testImplementation("org.assertj:assertj-core:${assertj}")
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
}

