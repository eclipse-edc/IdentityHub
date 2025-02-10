plugins {
    `java-library`
}

dependencies {
    api(project(":spi:issuerservice:issuerservice-participant-spi"))
    api(project(":spi:issuerservice:issuerservice-issuance-spi"))
    api(project(":spi:issuerservice:issuerservice-credential-definition-spi"))
    api(project(":core:lib:common-lib"))
    api(project(":core:lib:common-lib"))

    implementation(libs.edc.lib.store)
    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.lib.query)
    testImplementation(testFixtures(project(":spi:issuerservice:issuerservice-participant-spi")))
    testImplementation(testFixtures(project(":spi:issuerservice:issuerservice-issuance-spi")))

}
