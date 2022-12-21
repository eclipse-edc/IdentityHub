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
 *       Microsoft Corporation - initial implementation
 *
 */

plugins {
    `java-library`
}


dependencies {
    implementation(project(":core:identity-hub"))
    implementation(project(":core:identity-hub-client"))
    implementation(project(":spi:identity-hub-spi"))
    implementation(project(":extensions:credentials:identity-hub-credentials-jwt"));

    implementation(edc.spi.identity.did)
    implementation(libs.nimbus.jwt)
    implementation(libs.okhttp)


    testImplementation(testFixtures(project(":spi:identity-hub-spi")))
    testImplementation(project(":extensions:identity-hub-api"))
    testImplementation(project(":core:identity-hub-verifier"))

    testImplementation(edc.core.identity.did)
    testImplementation(edc.ext.identity.did.crypto)
    testImplementation(edc.core.junit)
}

publishing {
    publications {
        create<MavenPublication>("identity-hub-credentials-verifier") {
            artifactId = "identity-hub-credentials-verifier"
            from(components["java"])
        }
    }
}
