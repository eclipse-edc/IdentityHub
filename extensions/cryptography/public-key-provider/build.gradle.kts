plugins {
    `java-library`
}

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(libs.edc.spi.identity.did)
    implementation(libs.edc.identity.did.crypto)
    testImplementation(libs.edc.junit)
}
