plugins {
    `java-library`
}

dependencies {
    implementation(libs.edc.spi.core)
    implementation(libs.edc.util)
    testImplementation(libs.edc.junit)
}
