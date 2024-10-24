plugins {
    `java-library`
}

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(project(":spi:verifiable-credential-spi"))
    api(project(":spi:keypair-spi"))
    implementation(libs.edc.spi.token)
    implementation(libs.edc.spi.vc)
    implementation(libs.edc.spi.jwt)
    implementation(libs.edc.spi.jsonld)
    implementation(libs.edc.lib.token) // for Jwt generation service, token validation service and rule registry impl
    implementation(libs.edc.lib.common.crypto) // for the CryptoConverter
    implementation(libs.edc.lib.jws2020)
    implementation(libs.edc.vc.ldp)
    implementation(libs.edc.verifiablecredentials)

    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.lib.jsonld)
    testImplementation(testFixtures(libs.edc.vc.jwt)) // JWT generator
}
