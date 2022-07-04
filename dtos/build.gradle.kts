plugins {
    `java-library`
}

val jetBrainsAnnotationsVersion: String by project
val swagger: String by project

dependencies {
    api("org.jetbrains:annotations:${jetBrainsAnnotationsVersion}")
    implementation("io.swagger.core.v3:swagger-jaxrs2-jakarta:${swagger}")
}