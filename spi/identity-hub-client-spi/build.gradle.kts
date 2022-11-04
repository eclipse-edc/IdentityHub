/*
 *  Copyright (c) 2020, 2022 Microsoft Corporation
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


val edcVersion: String by project
val edcGroup: String by project
val nimbusVersion: String by project


dependencies {


    implementation("${edcGroup}:core-spi:${edcVersion}")
    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
}


publishing {
    publications {
        create<MavenPublication>("identity-hub-client-spi") {
            artifactId = "identity-hub-client-spi"
            from(components["java"])
        }
    }
}

