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
    `java-test-fixtures`
}


dependencies {
    testFixturesApi(libs.junit.jupiter.api)
    testFixturesApi(libs.edc.junit)
    testFixturesApi(libs.restAssured)
    testFixturesApi(libs.awaitility)
    testFixturesApi(libs.edc.transaction.local)
    testFixturesApi(libs.jakarta.rsApi)
    testFixturesApi(libs.testcontainers.junit)
    testFixturesApi(libs.testcontainers.postgres)
    testFixturesApi(testFixtures(libs.edc.sql.test.fixtures))

}

edcBuild {
    publish.set(false)
}