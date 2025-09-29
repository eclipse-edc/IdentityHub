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
}

val edcScmConnection: String by project
val edcScmUrl: String by project

buildscript {
    dependencies {
        val version: String by project
        classpath("org.eclipse.edc.autodoc:org.eclipse.edc.autodoc.gradle.plugin:$version")
    }
}

val edcBuildId = libs.plugins.edc.build.get().pluginId

allprojects {
    apply(plugin = edcBuildId)
    apply(plugin = "org.eclipse.edc.autodoc")

    configure<org.eclipse.edc.plugins.edcbuild.extensions.BuildExtension> {
        pom {
            scmConnection.set(edcScmConnection)
            scmUrl.set(edcScmUrl)
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
