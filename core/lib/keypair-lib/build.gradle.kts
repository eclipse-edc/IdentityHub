plugins {
    `java-library`
}

dependencies {
    api(libs.edc.lib.keys)
    implementation(project(":spi:keypair-spi"))
    implementation(libs.edc.spi.core)
    implementation(libs.edc.lib.util)
    testImplementation(libs.edc.junit)
    testImplementation(libs.nimbus.jwt)
}
