plugins {
    `java-library`
}

dependencies {
    api(project(":spi:issuerservice:issuerservice-participant-spi"))
    api(project(":spi:issuerservice:issuerservice-credential-definition-spi"))

    implementation(project(":core:lib:common-lib"))
    implementation(libs.edc.lib.store)
    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.lib.query)
    testImplementation(testFixtures(project(":spi:issuerservice:issuerservice-participant-spi")))
    testImplementation(testFixtures(project(":spi:issuerservice:issuerservice-credential-definition-spi")))
    testImplementation(testFixtures(project(":spi:issuance-credentials-spi")))
}
