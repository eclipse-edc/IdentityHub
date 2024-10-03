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

    testImplementation(project(":spi:participant-context-spi"))
    testImplementation(libs.edc.sts.spi)

    testImplementation(libs.edc.junit)
    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
}

edcBuild {
    publish.set(false)
}