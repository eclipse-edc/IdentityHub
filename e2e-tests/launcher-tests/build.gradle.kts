/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    testImplementation(libs.edc.junit)
    testImplementation(libs.testcontainers.junit)
}

edcBuild {
    publish.set(false)
}

tasks.test {
    // the tests build the launchers with a nested Gradle build. Building them here first makes that nested build find
    // everything up-to-date, so it does not build the same modules as this build at the same time when tasks run in
    // parallel. Only needed when the end-to-end tests run, so other test runs do not build the launchers for nothing.
    val includeTags = System.getProperty("includeTags").orEmpty().split(",")
    if ("EndToEndTest" in includeTags || System.getProperty("runAllTests").toBoolean()) {
        dependsOn(
            ":launcher:identityhub:shadowJar",
            ":launcher:identityhub-oauth2:shadowJar",
            ":launcher:issuer-service:shadowJar",
            ":launcher:issuer-service-oauth2:shadowJar"
        )
    }
}
