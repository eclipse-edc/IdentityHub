plugins {
    id("java")
}

group = "org.example"
version = "unspecified"

repositories {
    mavenCentral()
}

val nimbusVersion: String by project

dependencies {
    implementation(project(":spi:identity-hub-spi"))
    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}