plugins {
    `java-library`
}

dependencies {
    api(project(":spi:keypair-spi"))
    api(libs.edc.spi.core)
    implementation(libs.edc.lib.core)
    testImplementation(libs.edc.junit)
    testImplementation(libs.nimbus.jwt)
}
