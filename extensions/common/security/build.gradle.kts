plugins {
    `java-library`
}

dependencies {
    implementation(libs.edc.common.crypto) // for the crypto converter
    implementation(libs.nimbus.jwt)
    testImplementation(libs.edc.junit)
}
