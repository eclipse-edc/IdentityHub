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
    api(project(":dist:bom:issuerservice-base-bom"))
    // needed for interaction with the embedded STS
    runtimeOnly(project(":extensions:sts:sts-core"))
    runtimeOnly(project(":extensions:sts:sts-account-service-local"))
    runtimeOnly(project(":extensions:sts:sts-api"))
}

edcBuild {

}