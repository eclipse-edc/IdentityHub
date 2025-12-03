/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    implementation(project(":extensions:api:issuer-admin-api"))
    implementation(project(":core:common-core"))
    implementation(project(":core:identity-hub-did"))
    implementation(project(":core:identity-hub-participants"))
    implementation(project(":core:identity-hub-keypairs"))
    implementation(project(":core:issuerservice:issuerservice-core"))
    implementation(project(":core:issuerservice:issuerservice-holders"))
    implementation(project(":core:issuerservice:issuerservice-credentials"))
    implementation(project(":core:issuerservice:issuerservice-issuance"))
    implementation(project(":extensions:did:local-did-publisher"))

    implementation(project(":protocols:dcp:dcp-core"))
    implementation(project(":protocols:dcp:dcp-issuer:dcp-issuer-core"))
    implementation(project(":protocols:dcp:dcp-issuer:dcp-issuer-api"))
    implementation(project(":extensions:api:identity-api:participant-context-api"))

    implementation(project(":extensions:issuance:issuerservice-issuance-rules"))
    implementation(project(":extensions:issuance:issuerservice-holder-attestations"))

    implementation(project(":extensions:sts:sts-account-provisioner"))

    runtimeOnly(libs.edc.identity.did.core)
    runtimeOnly(libs.edc.core.token)
    runtimeOnly(libs.edc.core.participantcontext)
    runtimeOnly(libs.edc.api.version)
    runtimeOnly(libs.edc.identity.did.web)
    runtimeOnly(libs.bundles.connector)
}

edcBuild {

}
