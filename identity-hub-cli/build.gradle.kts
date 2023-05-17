plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
    `maven-publish`
}

dependencies {
    api(libs.picocli.core)
    annotationProcessor(libs.picocli.codegen)

    implementation(project(":core:identity-hub-client"))
    implementation(project(":extensions:credentials:identity-hub-credentials-jwt"))
    implementation(project(":extensions:identity-hub-verifier-jwt"))

    implementation(libs.edc.core.connector)
    implementation(libs.edc.ext.identity.did.crypto)
    implementation(libs.edc.spi.identity.did)
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
