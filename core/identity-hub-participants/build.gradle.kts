plugins {
    `java-library`
}

dependencies {
    api(project(":spi:did-spi"))
    api(project(":spi:participant-context-spi"))
    implementation(project(":spi:keypair-spi"))
    implementation(libs.edc.spi.core)
    implementation(libs.edc.spi.controlplane)
    api(libs.edc.spi.core)
    runtimeOnly(libs.bouncyCastle.bcprovJdk18on)
    implementation(libs.edc.spi.core)
    implementation(libs.opentelemetry.instrumentation.annotations)

    testImplementation(libs.edc.lib.keys)
    testImplementation(libs.edc.junit)
}
