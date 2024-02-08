plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.spi.core)
    api(project(":spi:identity-hub-spi"))
    api(project(":spi:identity-hub-did-spi"))
    implementation(libs.edc.util)

    testImplementation(libs.edc.junit)
}
