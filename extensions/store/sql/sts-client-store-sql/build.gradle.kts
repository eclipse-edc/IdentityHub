/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
}

dependencies {
    api(libs.edc.spi.core)

    implementation(libs.edc.lib.core)
    implementation(libs.edc.sql.bootstrapper)
    implementation(project(":spi:sts-spi"))
    testImplementation(libs.edc.junit)
    testImplementation(testFixtures(libs.edc.sql.test.fixtures))
    // needed by the concurrency tests: the standard test fixture auto-commits every statement, so a row is never held by
    // an open transaction
    testImplementation(libs.edc.transaction.local)
    testImplementation(testFixtures(project(":spi:sts-spi")))

}
