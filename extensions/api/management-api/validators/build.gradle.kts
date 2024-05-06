plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.spi.core)
    api(project(":spi:identity-hub-spi"))
    api(project(":spi:did-spi"))
    implementation(libs.edc.lib.util)

    testImplementation(libs.edc.junit)
}
