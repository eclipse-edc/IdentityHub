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

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(edc.spi.identity.did)
    testImplementation(project(":launcher"))
    testImplementation(project(":client-cli"))

    testImplementation(edc.core.junit)

    testImplementation(libs.picocli.core)
    testImplementation(libs.picocli.codegen)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.okhttp)
    testImplementation(libs.nimbus.jwt)
    testImplementation(libs.bouncycastle.bcpkix.jdk15on)
}

