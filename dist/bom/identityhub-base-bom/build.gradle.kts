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
    implementation(project(":core:common-core"))
    implementation(project(":core:identity-hub-did"))
    implementation(project(":core:identity-hub-core"))
    implementation(project(":core:identity-hub-participants"))
    implementation(project(":core:identity-hub-keypairs"))
    implementation(project(":extensions:did:local-did-publisher"))
    implementation(project(":extensions:credentials:credential-offer-handler"))
    implementation(project(":protocols:dcp:dcp-core"))
    implementation(project(":protocols:dcp:dcp-identityhub:presentation-api"))
    implementation(project(":protocols:dcp:dcp-identityhub:storage-api"))
    implementation(project(":protocols:dcp:dcp-identityhub:credential-offer-api"))
    implementation(project(":protocols:dcp:dcp-identityhub:dcp-identityhub-core"))

    implementation(project(":extensions:common:credential-watchdog"))
    implementation(project(":extensions:sts:sts-account-provisioner"))
    implementation(project(":extensions:api:identity-api:did-api"))
    implementation(project(":extensions:api:identity-api:participant-context-api"))
    implementation(project(":extensions:api:identity-api:verifiable-credentials-api"))
    implementation(project(":extensions:api:identity-api:keypair-api"))
    implementation(project(":extensions:api:identity-api:identity-api-configuration"))
    implementation(project(":extensions:api:identityhub-api-authentication"))
    implementation(project(":extensions:api:identityhub-api-authorization"))
    runtimeOnly(libs.edc.identity.did.core)
    runtimeOnly(libs.edc.core.token)
    runtimeOnly(libs.edc.api.version)
    runtimeOnly(libs.edc.transaction.local) // needed by the PresentationCreatorRegistry

    runtimeOnly(libs.edc.identity.did.web)
    runtimeOnly(libs.edc.jsonld)
    runtimeOnly(libs.bundles.connector)
}

edcBuild {

}
