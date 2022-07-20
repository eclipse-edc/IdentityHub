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
    java
    `java-library`
}

val projectVersion: String by project
val projectGroup: String by project
val swagger: String by project
val rsApi : String by project

allprojects {
    version = projectVersion
    group = projectGroup

    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://maven.iais.fraunhofer.de/artifactory/eis-ids-public/")
        }
        maven{
            url= uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
    }

    pluginManager.withPlugin("io.swagger.core.v3.swagger-gradle-plugin") {

        dependencies {
            // this is used to scan the classpath and generate an openapi yaml file
            implementation("io.swagger.core.v3:swagger-jaxrs2-jakarta:${swagger}")
            implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
        }
        // this is used to scan the classpath and generate an openapi yaml file
        tasks.withType<io.swagger.v3.plugins.gradle.tasks.ResolveTask> {
            outputFileName = project.name
            outputFormat = io.swagger.v3.plugins.gradle.tasks.ResolveTask.Format.YAML
            prettyPrint = true
            classpath = java.sourceSets["main"].runtimeClasspath
            buildClasspath = classpath
            resourcePackages = setOf("org.eclipse.dataspaceconnector")
            outputDir = file("${rootProject.projectDir.path}/resources/openapi/yaml")
        }
        configurations {
            all {
                exclude(group = "com.fasterxml.jackson.jaxrs", module = "jackson-jaxrs-json-provider")
            }
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
        }
    }

    // EdcRuntimeExtension uses this to determine the runtime classpath of the module to run.
    tasks.register("printClasspath") {
        doLast {
            println(sourceSets["main"].runtimeClasspath.asPath)
        }
    }

}
buildscript {
    dependencies {
        classpath("io.swagger.core.v3:swagger-gradle-plugin:2.1.13")
    }
}
