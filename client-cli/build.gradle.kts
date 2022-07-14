plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.0.0"
    `maven-publish`
}

val jacksonVersion: String by project
val jupiterVersion: String by project
val assertj: String by project
val mockitoVersion: String by project
val faker: String by project
val okHttpVersion: String by project
val nimbusVersion: String by project
val bouncycastleVersion: String by project

dependencies {
    api("info.picocli:picocli:4.6.3")
    annotationProcessor("info.picocli:picocli-codegen:4.6.3")

    implementation(project(":identity-hub-client"))
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
    implementation("org.bouncycastle:bcpkix-jdk15on:${bouncycastleVersion}")
    testImplementation("org.assertj:assertj-core:${assertj}")
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
    testImplementation("com.github.javafaker:javafaker:${faker}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
}

application {
    mainClass.set("org.eclipse.dataspaceconnector.identityhub.cli.IdentityHubCli")
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
