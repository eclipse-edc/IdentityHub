plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.spi.core)
    api(project(":spi:identity-hub-spi"))
    api(project(":spi:did-spi"))
    api(project(":spi:verifiable-credential-spi"))
    implementation(project(":extensions:api:identity-api:validators:keypair-validators"))
    implementation(libs.edc.lib.util)

    testImplementation(libs.edc.junit)
}
