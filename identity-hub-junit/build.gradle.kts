plugins {
    id("java")
}

group = "org.example"
version = "unspecified"

repositories {
    mavenCentral()
}

val nimbusVersion: String by project
val faker: String by project
val edcGroup: String by project
val edcVersion: String by project

dependencies {
    implementation(project(":spi:identity-hub-spi"))
    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
    implementation("com.github.javafaker:javafaker:${faker}")
    implementation("${edcGroup}:identity-did-spi:${edcVersion}")
    implementation("${edcGroup}:identity-did-crypto:${edcVersion}")
    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}