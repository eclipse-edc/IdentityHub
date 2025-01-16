/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    runtimeOnly(project(":core:identity-hub-did"))
    runtimeOnly(project(":core:identity-hub-core"))
    runtimeOnly(project(":core:identity-hub-participants"))
    runtimeOnly(project(":core:identity-hub-keypairs"))
    runtimeOnly(project(":extensions:did:local-did-publisher"))
    // API modules
    runtimeOnly(project(":extensions:protocols:dcp:credential-request-api"))
    runtimeOnly(project(":extensions:protocols:dcp:credential-request-status-api"))
    runtimeOnly(project(":extensions:protocols:dcp:issuer-metadata-api"))

    runtimeOnly(project(":extensions:sts:sts-account-provisioner"))
    runtimeOnly(libs.edc.identity.did.core)
    runtimeOnly(libs.edc.core.token)
    runtimeOnly(libs.edc.api.version)
    runtimeOnly(libs.edc.transaction.local) // needed by the PresentationCreatorRegistry

    runtimeOnly(libs.edc.identity.did.web)
    runtimeOnly(libs.bundles.connector)
}

edcBuild {

}