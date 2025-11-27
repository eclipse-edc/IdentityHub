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

    // sql modules
    implementation(project(":extensions:store:sql:identity-hub-credentials-store-sql"))
    implementation(project(":extensions:store:sql:identity-hub-did-store-sql"))
    implementation(project(":extensions:store:sql:identity-hub-keypair-store-sql"))
    implementation(project(":extensions:store:sql:holder-credential-request-store-sql"))
    implementation(project(":extensions:store:sql:holder-credential-offer-store-sql"))
    implementation(project(":extensions:store:sql:sts-client-store-sql"))

    runtimeOnly(libs.edc.sql.core)
    runtimeOnly(libs.edc.transaction.local)
    runtimeOnly(libs.edc.sql.pool)
    runtimeOnly(libs.edc.sql.bootstrapper)
    runtimeOnly(libs.edc.sql.jtivdalidation)
    runtimeOnly(libs.edc.sql.lease.core)
    runtimeOnly(libs.edc.sql.participantcontext)

    // third-party deps
    runtimeOnly(libs.postgres)
}
