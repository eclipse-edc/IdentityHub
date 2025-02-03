plugins {
    `java-library`
}

dependencies {
    implementation(libs.edc.spi.core)
    implementation(libs.edc.lib.query)
    testImplementation(libs.edc.junit)
    testImplementation(libs.nimbus.jwt)
}
