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
    api(project(":extensions:store:sql:identity-hub-credentials-store-sql"))
    api(project(":extensions:store:sql:identity-hub-did-store-sql"))
    api(project(":extensions:store:sql:identity-hub-keypair-store-sql"))
    api(project(":extensions:store:sql:identity-hub-participantcontext-store-sql"))

    api(libs.edc.sql.core)
    api(libs.edc.sql.pool)
    api(libs.edc.sql.transactionlocal)
    api(libs.edc.sql.bootstrapper)

    // third-party deps
    api(libs.postgres)
}