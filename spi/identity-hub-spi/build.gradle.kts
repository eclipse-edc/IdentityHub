plugins {
    `java-library`
    `maven-publish`
}

val jetBrainsAnnotationsVersion: String by project
val jacksonVersion: String by project

dependencies {
    api("org.jetbrains:annotations:${jetBrainsAnnotationsVersion}")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
}

publishing {
    publications {
        create<MavenPublication>("identity-hub-spi") {
            artifactId = "identity-hub-spi"
            from(components["java"])
        }
    }
}