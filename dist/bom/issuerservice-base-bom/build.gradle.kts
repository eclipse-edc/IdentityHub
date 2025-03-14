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
    runtimeOnly(project(":extensions:api:issuer-admin-api"))
    runtimeOnly(project(":core:common-core"))
    runtimeOnly(project(":core:identity-hub-did"))
    runtimeOnly(project(":core:identity-hub-participants"))
    runtimeOnly(project(":core:identity-hub-keypairs"))
    runtimeOnly(project(":core:issuerservice:issuerservice-core"))
    runtimeOnly(project(":core:issuerservice:issuerservice-holders"))
    runtimeOnly(project(":core:issuerservice:issuerservice-credentials"))
    runtimeOnly(project(":core:issuerservice:issuerservice-issuance"))
    runtimeOnly(project(":extensions:did:local-did-publisher"))
    // API modules
    runtimeOnly(project(":protocols:dcp:dcp-core"))
    runtimeOnly(project(":protocols:dcp:dcp-issuer:dcp-issuer-core"))
    runtimeOnly(project(":protocols:dcp:dcp-issuer:dcp-issuer-api"))
    runtimeOnly(project(":extensions:api:identity-api:participant-context-api"))

    runtimeOnly(project(":extensions:issuance:issuerservice-presentation-attestations"))
    runtimeOnly(project(":extensions:issuance:issuerservice-issuance-rules"))

    runtimeOnly(project(":extensions:sts:sts-account-provisioner"))
    runtimeOnly(project(":extensions:api:identityhub-api-authorization"))
    runtimeOnly(project(":extensions:api:issuer-admin-api:issuer-admin-api-authentication"))

    runtimeOnly(libs.edc.identity.did.core)
    runtimeOnly(libs.edc.core.token)
    runtimeOnly(libs.edc.api.version)

    runtimeOnly(libs.edc.identity.did.web)
    runtimeOnly(libs.bundles.connector)
}

edcBuild {

}