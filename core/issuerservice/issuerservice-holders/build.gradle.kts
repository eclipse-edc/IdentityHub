plugins {
    `java-library`
}

dependencies {
    api(project(":spi:issuerservice:issuerservice-holder-spi"))

    implementation(libs.edc.spi.core)
    implementation(libs.edc.lib.controlplane)
    testImplementation(libs.edc.junit)
    testImplementation(testFixtures(project(":spi:issuerservice:issuerservice-holder-spi")))
}
