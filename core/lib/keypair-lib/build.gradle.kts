plugins {
    `java-library`
}

dependencies {
    implementation(project(":spi:identity-hub-store-spi"))
    implementation(libs.edc.spi.core)
    implementation(libs.edc.lib.util)
    implementation(libs.edc.lib.keys)
    testImplementation(libs.edc.junit)
    testImplementation(libs.nimbus.jwt)
}
