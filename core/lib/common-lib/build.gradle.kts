plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    implementation(libs.edc.spi.core)
    implementation(libs.edc.lib.core)
    implementation(libs.edc.lib.controlplane)
    implementation(libs.nimbus.jwt)
    testImplementation(libs.edc.junit)

    testFixturesImplementation(libs.edc.spi.core)
}
