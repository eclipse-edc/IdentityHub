plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    implementation(libs.edc.spi.core)
    implementation(libs.edc.lib.core)
    implementation(libs.edc.lib.controlplane)
    implementation(libs.nimbus.jwt)
    implementation(libs.jakarta.rsApi)
    implementation(libs.jakarta.annotation)
    testImplementation(libs.edc.junit)
    testImplementation(testFixtures(libs.edc.core.jersey))

    testFixturesImplementation(libs.edc.spi.core)
}
