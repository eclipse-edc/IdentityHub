plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "8.0.0"
    `maven-publish`
}

dependencies {
    api(libs.picocli.core)
    annotationProcessor(libs.picocli.codegen)

    implementation(project(":core:identity-hub-client"))
    implementation(project(":extensions:credentials:identity-hub-credentials-jwt"))
    implementation(project(":extensions:identity-hub-verifier-jwt"))

    implementation(edc.core.connector)
    implementation(edc.ext.identity.did.crypto)
    implementation(edc.spi.identity.did)
    implementation(libs.jackson.databind)
    implementation(libs.nimbus.jwt)

    testImplementation(testFixtures(project(":spi:identity-hub-spi")))
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
