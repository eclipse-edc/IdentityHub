plugins {
    `java-library`
}

dependencies {
    api(project(":spi:verifiable-credential-spi"))
    api(project(":spi:issuerservice:issuerservice-credential-spi"))
    api(project(":spi:issuerservice:issuerservice-holder-spi"))
    api(project(":spi:issuerservice:issuerservice-issuance-spi"))
    api(project(":spi:identity-hub-spi"))
    api(project(":protocols:dcp:dcp-spi")) // CredentialOfferMessage
    api(libs.edc.spi.core) // for the Request
    implementation(project(":protocols:dcp:dcp-issuer:dcp-issuer-spi"))
    implementation(libs.edc.lib.token)
    implementation(libs.nimbus.jwt)


    implementation(libs.edc.spi.core)
    testImplementation(libs.edc.junit)

}
