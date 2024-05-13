plugins {
    `java-library`
}

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(project(":spi:verifiable-credential-spi"))
    api(project(":spi:keypair-spi"))
    implementation(libs.edc.spi.token)
    implementation(libs.edc.spi.vc)
    implementation(libs.edc.spi.jsonld)
    implementation(libs.edc.core.token) // for Jwt generation service, token validation service and rule registry impl
    implementation(libs.edc.common.crypto) // for the CryptoConverter
    implementation(libs.edc.lib.jws2020)
    implementation(libs.edc.vc.ldp)
    implementation(libs.edc.verifiablecredentials)

    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.lib.jsonld)
    testImplementation(testFixtures(project(":spi:identity-hub-spi")))
    testImplementation(testFixtures(libs.edc.vc.jwt)) // JWT generator
}
