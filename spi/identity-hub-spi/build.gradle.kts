plugins {
    `java-library`
}

val jetBrainsAnnotationsVersion: String by project
val jacksonVersion: String by project

dependencies {
    api("org.jetbrains:annotations:${jetBrainsAnnotationsVersion}")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
}