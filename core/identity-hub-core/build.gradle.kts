plugins {
    `java-library`
}

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(project(":spi:identity-hub-store-spi"))
    implementation(project(":core:lib:verifiable-presentation-lib"))
    implementation(project(":core:lib:accesstoken-lib"))
    implementation(project(":core:lib:credential-query-lib"))
    implementation(libs.edc.spi.dcp) //SignatureSuiteRegistry
    implementation(libs.edc.core.connector) // for the CriterionToPredicateConverterImpl
    implementation(libs.edc.jsonld) // for the JSON-LD mapper
    implementation(libs.edc.lib.util)
    implementation(libs.edc.lib.store)
    implementation(libs.edc.lib.jsonld)
    implementation(libs.edc.lib.query)
    implementation(libs.edc.lib.jws2020)
    implementation(libs.edc.lib.common.crypto)
    implementation(libs.edc.spi.token)
    implementation(libs.edc.spi.identity.did)
    implementation(libs.edc.vc.ldp)
    implementation(libs.edc.core.token)
    implementation(libs.edc.verifiablecredentials) // revocation list service


    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.jsonld)
    testImplementation(testFixtures(project(":spi:identity-hub-store-spi")))
    testImplementation(testFixtures(libs.edc.vc.jwt)) // JWT generator
}
