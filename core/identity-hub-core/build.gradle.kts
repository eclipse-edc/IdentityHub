plugins {
    `java-library`
}

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(project(":spi:identity-hub-store-spi"))
    implementation(libs.edc.core.connector) // for the CriterionToPredicateConverterImpl
    implementation(libs.edc.spi.jsonld)
    implementation(libs.edc.iatp.service) // JWT validator
    implementation(libs.edc.core.crypto) // JWT verifier
    implementation(libs.edc.jws2020)
    implementation(libs.edc.vc.ldp)
    implementation(libs.edc.util)
    implementation(libs.nimbus.jwt)

    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.ext.jsonld)
    testImplementation(testFixtures(project(":spi:identity-hub-spi")))
    testImplementation(testFixtures(libs.edc.vc.jwt)) // JWT generator
    testImplementation(libs.edc.identity.did.crypto) // EC private key wrapper
}
