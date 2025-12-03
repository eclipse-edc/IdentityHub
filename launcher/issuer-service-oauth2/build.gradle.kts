/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */


plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
}

dependencies {
    runtimeOnly(project(":dist:bom:issuerservice-oauth2-bom"))
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.shadowJar {
    mergeServiceFiles()
    archiveFileName.set("issuer-service-oauth2.jar")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

edcBuild {
    publish.set(false)
}
