/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
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
    api(project(":spi:did-spi"))
    implementation(libs.edc.core.sql) // for the SqlStatements
    implementation(libs.edc.spi.transaction.datasource)

    testImplementation(testFixtures(project(":spi:did-spi")))
    testImplementation(testFixtures(libs.edc.core.sql))
    testImplementation(libs.edc.junit)
}
