plugins {
    `java-library`
}

dependencies {
    api(project(":spi:keypair-spi"))
    api(libs.edc.spi.keys)
    implementation(libs.edc.spi.core)
    implementation(libs.edc.lib.util)
    testImplementation(libs.edc.lib.keys)
    testImplementation(libs.edc.junit)
    testImplementation(libs.nimbus.jwt)
}
