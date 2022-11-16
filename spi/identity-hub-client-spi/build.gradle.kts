/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
}

dependencies {
    api(edc.spi.core)
    implementation(libs.nimbus.jwt)
}


publishing {
    publications {
        create<MavenPublication>("identity-hub-client-spi") {
            artifactId = "identity-hub-client-spi"
            from(components["java"])
        }
    }
}

