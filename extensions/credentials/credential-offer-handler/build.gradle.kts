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
    `java-test-fixtures`
    `maven-publish`
}

dependencies {

    implementation(project(":spi:identity-hub-spi"))
    implementation(project(":spi:verifiable-credential-spi"))
    implementation(project(":protocols:dcp:dcp-spi"))
    implementation(libs.edc.spi.transaction)
    
    testImplementation(libs.edc.junit)
    testImplementation(libs.restAssured)
}
