plugins {
    `java-library`
}

dependencies {
    api(project(":spi:verifiable-credential-spi"))
    api(project(":spi:issuerservice:issuerservice-credential-spi"))
    implementation(libs.edc.lib.token)
    implementation(libs.nimbus.jwt)


    implementation(libs.edc.spi.transaction)
    implementation(libs.edc.lib.store)
    testImplementation(libs.edc.junit)

}
