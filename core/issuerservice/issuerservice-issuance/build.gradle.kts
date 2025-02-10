plugins {
    `java-library`
}

dependencies {
    api(project(":spi:issuerservice:issuerservice-issuance-spi"))
    api(project(":spi:issuerservice:issuerservice-participant-spi"))
    implementation(project(":core:lib:common-lib"))
    implementation(libs.edc.spi.transaction)
    implementation(libs.edc.lib.store)
    implementation(libs.edc.lib.statemachine)
    testImplementation(libs.edc.junit)
    testImplementation(testFixtures(project(":spi:issuerservice:issuerservice-issuance-spi")))

}
