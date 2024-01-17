plugins {
    `java-library`
}

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(project(":spi:identity-hub-store-spi"))
    api(libs.edc.spi.transaction)

    testImplementation(libs.edc.junit)
}
