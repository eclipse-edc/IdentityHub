plugins {
    `java-library`
    `maven-publish`
}

val jetBrainsAnnotationsVersion: String by project
val swagger: String by project

dependencies {
    api("org.jetbrains:annotations:${jetBrainsAnnotationsVersion}")
    implementation("io.swagger.core.v3:swagger-jaxrs2-jakarta:${swagger}") {
        exclude(group = "com.fasterxml.jackson.jaxrs", module = "jackson-jaxrs-json-provider")
    }
}

publishing {
    publications {
        create<MavenPublication>("identity-hub-store-dtos") {
            artifactId = "identity-hub-client-store-dtos"
            from(components["java"])
        }
    }
}