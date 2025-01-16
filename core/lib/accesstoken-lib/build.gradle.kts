plugins {
    `java-library`
}

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(project(":core:lib:keypair-lib")) // for the KeyPairResourcePublicKeyResolver
    implementation(libs.edc.spi.token)
    implementation(libs.edc.spi.jwt)

    testImplementation(libs.edc.junit)
    testImplementation(libs.nimbus.jwt)
    testImplementation(libs.edc.vc.jwt) // JtiValidationRule
    testImplementation(libs.edc.lib.token) // TokenValidationServiceImpl
}
