plugins {
    `java-library`
}

dependencies {
    api(project(":spi:verifiable-credential-spi"))
    api(project(":spi:issuerservice:issuerservice-credential-spi"))
    api(project(":spi:issuerservice:issuerservice-holder-spi"))
    api(project(":spi:issuerservice:issuerservice-issuance-spi"))
    api(project(":spi:identity-hub-spi"))
    api(libs.edc.spi.http) // for the Request
    implementation(libs.edc.lib.token)
    implementation(libs.nimbus.jwt)


    implementation(libs.edc.spi.transaction)
    testImplementation(libs.edc.junit)

}
