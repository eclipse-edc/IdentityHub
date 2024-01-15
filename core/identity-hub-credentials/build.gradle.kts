plugins {
    `java-library`
}

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(project(":spi:identity-hub-store-spi"))
    implementation(libs.edc.spi.token)
    implementation(libs.edc.core.token) // for Jwt generation service, token validation service and rule registry impl
    implementation(libs.edc.core.connector) // for the CriterionToPredicateConverterImpl
    implementation(libs.edc.common.crypto) // for the crypto converter
    implementation(libs.edc.ext.jsonld) // for the JSON-LD mapper
    implementation(libs.edc.jws2020)
    implementation(libs.edc.vc.ldp)
    implementation(libs.edc.util)
    implementation(libs.nimbus.jwt)

    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.ext.jsonld)
    testImplementation(testFixtures(project(":spi:identity-hub-spi")))
    testImplementation(testFixtures(project(":spi:identity-hub-store-spi")))
    testImplementation(testFixtures(libs.edc.vc.jwt)) // JWT generator
}
