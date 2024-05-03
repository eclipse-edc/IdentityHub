plugins {
    `java-library`
}

dependencies {
    implementation(libs.edc.spi.core)
    implementation(libs.edc.lib.util)
    testImplementation(libs.edc.junit)
}
