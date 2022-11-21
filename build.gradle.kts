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
}


val edcDeveloperId: String by project
val edcDeveloperName: String by project
val edcDeveloperEmail: String by project
val edcScmConnection: String by project
val edcWebsiteUrl: String by project
val edcScmUrl: String by project
val edcGroup: String by project
val annotationProcessorVersion: String by project
val metaModelVersion: String by project
val javaVersion: String by project

val defaultVersion: String by project

// makes the project version overridable using the "-PidentityHubVersion..." flag. Useful for CI builds
val actualVersion: String = (project.findProperty("identityHubVersion") ?: defaultVersion) as String

buildscript {
    dependencies {
        val edcGradlePluginsVersion: String by project
        classpath("org.eclipse.edc.edc-build:org.eclipse.edc.edc-build.gradle.plugin:${edcGradlePluginsVersion}")
    }
}


allprojects {
    apply(plugin = "${edcGroup}.edc-build")

    // configure which version of the annotation processor to use. defaults to the same version as the plugin
    configure<org.eclipse.edc.plugins.autodoc.AutodocExtension> {
        processorVersion.set(annotationProcessorVersion)
        outputDirectory.set(project.buildDir)
    }

    configure<org.eclipse.edc.plugins.edcbuild.extensions.BuildExtension> {
        versions {
            // override default dependency versions here
            projectVersion.set(actualVersion)
            metaModel.set(metaModelVersion)

        }
        pom {
            projectName.set(project.name)
            description.set("edc :: ${project.name}")
            projectUrl.set(edcWebsiteUrl)
            scmConnection.set(edcScmConnection)
            scmUrl.set(edcScmUrl)
        }
        swagger {
            title.set("Identity HUB REST API")
            description = "Identity HUB REST APIs - merged by OpenApiMerger"
            outputFilename.set(project.name)
            outputDirectory.set(file("${rootProject.projectDir.path}/resources/openapi/yaml"))
        }
        javaLanguageVersion.set(JavaLanguageVersion.of(javaVersion))
    }

    configure<CheckstyleExtension> {
        configFile = rootProject.file("resources/checkstyle-config.xml")
        configDirectory.set(rootProject.file("resources"))
    }


    // EdcRuntimeExtension uses this to determine the runtime classpath of the module to run.
    tasks.register("printClasspath") {
        doLast {
            println(sourceSets["main"].runtimeClasspath.asPath)
        }
    }

}

