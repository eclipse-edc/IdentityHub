plugins {
    `java-library`
}

dependencies {
    api(project(":spi:did-spi"))
    api(project(":spi:participant-context-spi"))
    api(libs.edc.spi.core)
    implementation(project(":spi:keypair-spi"))
    implementation(libs.edc.spi.controlplane)
    runtimeOnly(libs.bouncyCastle.bcprovJdk18on)
    implementation(libs.opentelemetry.instrumentation.annotations)

    testImplementation(libs.edc.lib.core)
    testImplementation(libs.edc.junit)
}
