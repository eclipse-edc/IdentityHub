plugins {
    `java-library`
}

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(project(":spi:did-spi"))
    api(project(":spi:identity-hub-store-spi"))
    api(libs.edc.spi.transaction)
    implementation(project(":core:lib:keypair-lib"))
    implementation(libs.edc.common.crypto)
    implementation(libs.edc.core.connector)

    testImplementation(libs.edc.lib.keys)
    testImplementation(libs.edc.junit)
}
