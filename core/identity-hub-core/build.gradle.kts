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
    implementation(libs.nimbus.jwt)

    testImplementation(libs.edc.junit)
    testImplementation(testFixtures(project(":spi:identity-hub-spi")))
}
