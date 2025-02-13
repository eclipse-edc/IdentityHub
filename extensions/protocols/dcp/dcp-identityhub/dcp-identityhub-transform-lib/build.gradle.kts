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
    api(libs.edc.spi.jsonld)
    api(libs.edc.spi.dcp)
    api(project(":extensions:protocols:dcp:dcp-spi"))
    testImplementation(libs.edc.lib.jsonld)
    testImplementation(libs.edc.lib.transform)

}

