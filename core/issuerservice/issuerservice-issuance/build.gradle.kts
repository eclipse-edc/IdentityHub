plugins {
    `java-library`
}

dependencies {
    api(project(":spi:issuerservice:issuerservice-issuance-spi"))
    api(project(":spi:issuerservice:issuerservice-credential-spi")) // for the CredentialStatusService
    api(project(":spi:issuerservice:issuerservice-holder-spi"))
    api(project(":spi:verifiable-credential-spi"))
    api(project(":spi:keypair-spi"))
    implementation(project(":core:lib:common-lib"))
    implementation(project(":core:lib:issuerservice-common-lib"))
    implementation(libs.edc.spi.core)
    implementation(libs.edc.lib.core)
    implementation(libs.edc.lib.controlplane)
    implementation(libs.opentelemetry.instrumentation.annotations)

    testImplementation(libs.edc.junit)
    testImplementation(testFixtures(project(":spi:issuerservice:issuerservice-issuance-spi")))
    testImplementation(libs.awaitility)
}
