plugins {
    `java-library`
}

dependencies {
    api(project(":spi:did-spi"))
    api(project(":spi:participant-context-spi"))

    implementation(project(":spi:keypair-spi"))
    implementation(libs.edc.spi.transaction)
    implementation(libs.edc.lib.common.crypto)
    implementation(libs.edc.lib.store)

    testImplementation(libs.edc.lib.query)
    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.lib.keys)
    testImplementation(testFixtures(project(":spi:did-spi")))
    testRuntimeOnly(libs.edc.jsonld)
}
