plugins {
    `java-library`
}

dependencies {
    api(project(":spi:issuerservice:issuerservice-holder-spi"))

    implementation(libs.edc.spi.transaction)
    implementation(libs.edc.lib.store)
    testImplementation(libs.edc.junit)
    testImplementation(testFixtures(project(":spi:issuerservice:issuerservice-holder-spi")))
}
