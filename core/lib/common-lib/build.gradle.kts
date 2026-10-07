plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    implementation(libs.edc.spi.core)
    implementation(libs.edc.lib.core)
    testImplementation(libs.edc.junit)
    testImplementation(libs.nimbus.jwt)

    testFixturesImplementation(libs.edc.spi.core)
}
