plugins {
    `java-library`
}

dependencies {
    api(project(":spi:identity-hub-store-spi"))

    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.core.token)
    testImplementation(libs.nimbus.jwt)
    testImplementation(testFixtures(project(":spi:identity-hub-spi")))

}
