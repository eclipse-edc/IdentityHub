plugins {
    `java-library`
}

dependencies {
    api(project(":spi:identity-hub-spi"))

    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.ext.jsonld)
    testImplementation(testFixtures(project(":spi:identity-hub-spi")))
    testImplementation(libs.edc.identity.did.crypto) // EC private key wrapper
}
