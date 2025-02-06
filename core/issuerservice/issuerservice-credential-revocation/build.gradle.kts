plugins {
    `java-library`
}

dependencies {
    api(project(":spi:verifiable-credential-spi"))
    api(project(":spi:issuerservice:credential-revocation-spi"))
    implementation(libs.nimbus.jwt)

    implementation(libs.edc.spi.transaction)
    implementation(libs.edc.lib.store)
    testImplementation(project(":core:identity-hub-core"))
    testImplementation(libs.edc.junit)
    testImplementation(testFixtures(project(":spi:issuerservice:issuerservice-participant-spi")))

}
