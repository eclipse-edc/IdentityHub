plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:did-spi"))
    api(libs.edc.spi.core)
    implementation(libs.edc.lib.core)

    testImplementation(libs.edc.junit)
}
