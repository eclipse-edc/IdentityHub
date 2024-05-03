/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
}

val swagger: String by project

dependencies {

    api(project(":spi:participant-context-spi"))

    testImplementation(libs.edc.lib.json)
    testFixturesImplementation(libs.nimbus.jwt)
    testFixturesImplementation(libs.edc.spi.identity.did)
}
