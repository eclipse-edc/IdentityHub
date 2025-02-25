/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

plugins {
    `java-library`
}


dependencies {
    // required modules
    runtimeOnly(libs.bundles.connector)
    runtimeOnly(libs.edc.sts.spi)
    runtimeOnly(project(":extensions:sts:sts-api"))
    runtimeOnly(project(":extensions:sts:sts-core"))
    runtimeOnly(libs.edc.core.token)
    runtimeOnly(libs.edc.core.api)

    // optional modules
    runtimeOnly(libs.edc.api.version)
}

edcBuild {
    publish.set(false)
}