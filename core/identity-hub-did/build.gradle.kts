plugins {
    `java-library`
}

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(project(":spi:identity-hub-did-spi"))

    implementation(project(":spi:keypair-spi"))
    implementation(project(":spi:participant-context-spi"))
    implementation(libs.edc.core.connector) // for the reflection-based query resolver
    implementation(libs.edc.common.crypto)
    implementation(libs.edc.lib.store)
    implementation(libs.edc.lib.query)

    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.ext.jsonld)
    testImplementation(libs.edc.lib.keys)
    testImplementation(testFixtures(project(":spi:identity-hub-spi")))
    testImplementation(testFixtures(project(":spi:identity-hub-did-spi")))
}
