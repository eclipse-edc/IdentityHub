plugins {
    `java-library`
}

dependencies {
    api(project(":spi:identity-hub-spi"))
    implementation(libs.edc.spi.token)
    implementation(libs.edc.spi.jwt)

    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.core.token)
    testImplementation(libs.nimbus.jwt)
    testImplementation(testFixtures(project(":spi:identity-hub-spi")))

}
