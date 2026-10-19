plugins {
    `java-library`
}

dependencies {
    api(project(":spi:keypair-spi"))
    api(libs.edc.spi.core)
    api(libs.nimbus.jwt) // for the PublicKeyJwk
    implementation(libs.edc.lib.core)
    testImplementation(libs.edc.junit)
}
