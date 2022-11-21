plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.0.0"
    `maven-publish`
}

dependencies {
    api(libs.picocli.core)
    annotationProcessor(libs.picocli.codegen)

    implementation(project(":core:identity-hub-client"))
    implementation(edc.spi.identity.did)
    implementation(libs.jackson.databind)
    implementation(libs.okhttp)
    implementation(libs.nimbus.jwt)
    implementation(libs.bouncycastle.bcpkix.jdk15on)
}
repositories {
    mavenCentral()
}

application {
    mainClass.set("org.eclipse.edc.identityhub.cli.IdentityHubCli")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("identity-hub-cli.jar")
}

publishing {
    publications {
        create<MavenPublication>("identity-hub-cli") {
            artifactId = "identity-hub-cli"
            from(components["java"])
        }
    }
}
