/*
 *  Copyright (c) 2026 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft - initial API and implementation
 *
 */

plugins {
    `java-library`
}

// identity-did-spi 0.18.0 has no capabilityInvocation. This project replaces that jar's
// DidDocument and keeps the other classes. It is excluded from the dependency substitution
// so it can still resolve the upstream artifact.

val edcVersion = project.property("con-x-edcVersion") as String

val upstreamSpi: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    add("upstreamSpi", "org.eclipse.edc:identity-did-spi:$edcVersion")
    compileOnly("org.eclipse.edc:identity-did-spi:$edcVersion")
    api(libs.edc.spi.core)
    api(libs.edc.spi.keys)
    api(libs.nimbus.jwt)
}

tasks.named<JavaCompile>("compileJava") {
    inputs.files(upstreamSpi)
    doLast {
        copy {
            from(zipTree(upstreamSpi.singleFile)) {
                exclude("META-INF/MANIFEST.MF")
                exclude("org/eclipse/edc/iam/did/spi/document/DidDocument.class")
                exclude("org/eclipse/edc/iam/did/spi/document/DidDocument\$Builder.class")
            }
            into(destinationDirectory)
        }
    }
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
