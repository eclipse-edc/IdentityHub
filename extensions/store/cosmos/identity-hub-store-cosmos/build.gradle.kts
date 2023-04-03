/*
 *  Copyright (c) 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:identity-hub-store-spi"))
    api(libs.edc.ext.azure.cosmos.core)

    implementation(root.failsafe.core)
    implementation(root.azure.cosmos)

    testImplementation(testFixtures(project(":spi:identity-hub-store-spi")))
    testImplementation(testFixtures(libs.edc.ext.azure.test))

}
