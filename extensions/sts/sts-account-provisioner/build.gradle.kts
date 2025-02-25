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
    `maven-publish`
}

dependencies {

    implementation(project(":spi:sts-spi"))
    implementation(libs.edc.spi.core)
    implementation(project(":spi:participant-context-spi"))
    implementation(project(":spi:keypair-spi"))
    implementation(project(":spi:did-spi"))
    testImplementation(libs.edc.junit)
}
