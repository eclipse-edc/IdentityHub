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

    configurations.all {
        // io.nats:jnats, pulled in transitively via EDC's core-lib, ships the BouncyCastle LTS provider. It contains the
        // same org.bouncycastle packages as bcprov-jdk18on, so having both on the classpath makes provider
        // initialization fail with NoSuchFieldError on algorithm identifiers that only one of them declares.
        exclude(group = "org.bouncycastle", module = "bcprov-lts8on")

        resolutionStrategy {
            // BouncyCastle releases patch versions of the provider without the accompanying bcutil/bcpkix, so picking the
            // highest requested bcprov (the DCP TCK asks for one) splits the family and fails the same way. Keeping the
            // provider on the version that bcutil and bcpkix are published for is what keeps the classpath coherent.
            force("org.bouncycastle:bcprov-jdk18on:${rootProject.libs.versions.bouncyCastle.jdk18on.get()}")
        }
    }

}

tasks.withType(MergeOpenApiSpecTask::class.java) {
    skipOperationExample.set(true)
}
