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
    api(project(":spi:keypair-spi"))
    api(libs.edc.spi.transaction)
    implementation(project(":core:lib:keypair-lib"))
    implementation(libs.edc.lib.common.crypto)
    runtimeOnly(libs.edc.core.connector)
    testImplementation(libs.edc.junit)
}
