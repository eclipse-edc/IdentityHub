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
}
val assertj: String by project
val edcVersion: String by project
val edcGroup: String by project
val jupiterVersion: String by project
val restAssured: String by project
val faker: String by project

dependencies {
    implementation("${edcGroup}:core:${edcVersion}")
    implementation(project(":extensions:identity-hub-store-spi"))

    testImplementation("${edcGroup}:http:${edcVersion}")
    testImplementation("${edcGroup}:common-util:${edcVersion}")
    implementation("${edcGroup}:common-util:${edcVersion}:test-fixtures")
    testImplementation(project(":rest-client"))
    testImplementation("${edcGroup}:junit-extension:${edcVersion}:test-fixtures")
    testImplementation("${edcGroup}:common-util:${edcVersion}:test-fixtures")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testImplementation("org.assertj:assertj-core:${assertj}")
    testImplementation("io.rest-assured:rest-assured:${restAssured}")
    testImplementation("com.github.javafaker:javafaker:${faker}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
}
