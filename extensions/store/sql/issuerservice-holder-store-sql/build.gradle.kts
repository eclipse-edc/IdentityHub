/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:issuerservice:issuerservice-holder-spi"))
    implementation(libs.edc.lib.core)
    implementation(libs.edc.sql.bootstrapper)
    implementation(libs.edc.spi.core)

    testImplementation(testFixtures(project(":spi:issuerservice:issuerservice-holder-spi")))
    testImplementation(testFixtures(libs.edc.sql.test.fixtures))
    // needed by the concurrency tests: the standard test fixture auto-commits every statement, so a row is never held by
    // an open transaction
    testImplementation(libs.edc.transaction.local)
    testImplementation(libs.edc.junit)
}
