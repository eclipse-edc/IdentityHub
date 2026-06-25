plugins {
    `java-library`
}

dependencies {
    api(project(":spi:did-spi"))
    api(project(":spi:participant-context-spi"))

    implementation(project(":spi:keypair-spi"))
    implementation(libs.edc.spi.core)
    implementation(libs.edc.lib.core)
    implementation(libs.edc.lib.controlplane)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.instrumentation.annotations)

    testImplementation(libs.edc.junit)
    testImplementation(testFixtures(project(":spi:did-spi")))
    testRuntimeOnly(libs.edc.jsonld)
}
