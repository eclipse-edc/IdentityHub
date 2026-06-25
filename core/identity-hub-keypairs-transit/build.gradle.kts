/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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
    api(libs.edc.spi.core)
    implementation(project(":core:lib:keypair-lib"))
    implementation(libs.edc.lib.common.crypto)
    implementation(libs.edc.lib.token)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.instrumentation.annotations)
    implementation(libs.bouncyCastle.bcprovJdk18on)
    implementation(libs.bouncyCastle.bcpkixJdk18on)
    runtimeOnly(libs.edc.core.connector)
    runtimeOnly(libs.edc.vault.hashicorp)
    testImplementation(libs.edc.junit)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.vault)
}
