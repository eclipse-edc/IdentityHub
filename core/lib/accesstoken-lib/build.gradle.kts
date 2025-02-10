plugins {
    `java-library`
}

dependencies {
    api(project(":core:lib:keypair-lib")) // for the KeyPairResourcePublicKeyResolver
    api(libs.edc.spi.token)
    implementation(libs.edc.spi.jwt)

    testImplementation(libs.edc.junit)
    testImplementation(libs.nimbus.jwt)
    testImplementation(libs.edc.vc.jwt) // JtiValidationRule
}
