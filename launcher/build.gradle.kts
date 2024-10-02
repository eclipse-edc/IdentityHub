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
 *       Amadeus - initial API and implementation
 *
 */

plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
}

dependencies {
    runtimeOnly(project(":core:presentation-api"))
    runtimeOnly(project(":core:identity-hub-did"))
    runtimeOnly(project(":core:identity-hub-core"))
    runtimeOnly(project(":core:identity-hub-participants"))
    runtimeOnly(project(":core:identity-hub-keypairs"))
    runtimeOnly(project(":extensions:did:local-did-publisher"))
    runtimeOnly(project(":extensions:common:credential-watchdog"))
    runtimeOnly(project(":extensions:sts:sts-account-provisioner"))
    runtimeOnly(project(":extensions:sts:sts-account-service-local"))
    runtimeOnly(project(":extensions:api:identity-api:did-api"))
    runtimeOnly(project(":extensions:api:identity-api:participant-context-api"))
    runtimeOnly(project(":extensions:api:identity-api:verifiable-credentials-api"))
    runtimeOnly(project(":extensions:api:identity-api:keypair-api"))
    runtimeOnly(project(":extensions:api:identity-api:api-configuration"))
    runtimeOnly(project(":extensions:api:identityhub-api-authentication"))
    runtimeOnly(project(":extensions:api:identityhub-api-authorization"))
    runtimeOnly(libs.edc.identity.did.core)
    runtimeOnly(libs.edc.core.token)
    runtimeOnly(libs.edc.api.version)
    runtimeOnly(libs.edc.sts.core)
    runtimeOnly(libs.edc.sts)
    runtimeOnly(libs.edc.sts.api)

    runtimeOnly(libs.edc.identity.did.web)
    runtimeOnly(libs.bundles.connector)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("identity-hub.jar")
}

edcBuild {
    publish.set(false)
}
