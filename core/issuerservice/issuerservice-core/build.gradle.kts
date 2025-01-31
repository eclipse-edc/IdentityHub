plugins {
    `java-library`
}

dependencies {
    api(project(":spi:issuerservice:issuerservice-participant-spi"))

    implementation(project(":core:lib:common-lib"))
    implementation(libs.edc.lib.store)
    testImplementation(libs.edc.junit)
    testImplementation(testFixtures(project(":spi:issuerservice:issuerservice-participant-spi")))

}
