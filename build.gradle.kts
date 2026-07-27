import org.eclipse.edc.plugins.edcbuild.plugins.MergeOpenApiSpecTask

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
 *       Microsoft Corporation - initial implementation
 *
 */

plugins {
    `java-library`
    alias(libs.plugins.edc.build)
    alias(libs.plugins.autodoc) apply false
}

allprojects {
    apply(plugin = rootProject.libs.plugins.edc.build.get().pluginId)
    apply(plugin = rootProject.libs.plugins.autodoc.get().pluginId)

    configure<org.eclipse.edc.plugins.edcbuild.extensions.BuildExtension> {
        pom {
            scmConnection.set(rootProject.property("edcScmConnection") as String)
            scmUrl.set(rootProject.property("edcScmUrl") as String)
        }
    }

    configure<CheckstyleExtension> {
        configFile = rootProject.file("resources/checkstyle-config.xml")
        configDirectory.set(rootProject.file("resources"))
    }

}

tasks.withType(MergeOpenApiSpecTask::class.java) {
    skipOperationExample.set(true)
}
