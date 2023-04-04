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

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(project(":spi:identity-hub-client-spi"))
    api(libs.edc.spi.http)
    api(libs.edc.spi.core)

    testImplementation(project(":core:identity-hub"))
    testImplementation(project(":extensions:identity-hub-api"))
    testImplementation(project(":extensions:credentials:identity-hub-credentials-jwt"))
    testImplementation(testFixtures(project(":spi:identity-hub-spi")))
    testImplementation(libs.edc.core.junit)
    testImplementation(libs.edc.ext.identity.did.core)
}
