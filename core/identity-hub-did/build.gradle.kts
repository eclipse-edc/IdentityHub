plugins {
    `java-library`
}

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(project(":spi:did-spi"))

    implementation(project(":spi:keypair-spi"))
    implementation(project(":spi:identity-hub-store-spi"))
    implementation(project(":spi:participant-context-spi"))
    implementation(libs.edc.core.connector) // for the reflection-based query resolver
    implementation(libs.edc.lib.common.crypto)
    implementation(libs.edc.lib.store)
    implementation(libs.edc.lib.query)

    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.jsonld)
    testImplementation(libs.edc.lib.keys)
    testImplementation(testFixtures(project(":spi:identity-hub-spi")))
    testImplementation(testFixtures(project(":spi:did-spi")))
}
