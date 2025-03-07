plugins {
    `java-library`
}

dependencies {
    api(project(":spi:verifiable-credential-spi"))
    api(project(":spi:issuerservice:issuerservice-credential-spi"))
    api(project(":spi:issuerservice:issuerservice-issuance-spi"))
    implementation(libs.edc.lib.token)
    implementation(libs.nimbus.jwt)


    implementation(libs.edc.spi.transaction)
    testImplementation(libs.edc.junit)

}
