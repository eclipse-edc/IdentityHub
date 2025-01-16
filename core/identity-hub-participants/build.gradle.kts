plugins {
    `java-library`
}

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(project(":spi:did-spi"))
    api(project(":spi:participant-context-spi"))
    api(project(":spi:keypair-spi"))
    api(libs.edc.spi.transaction)
    implementation(project(":core:lib:keypair-lib"))
    implementation(libs.edc.lib.common.crypto)
    implementation(libs.edc.core.connector)

    testImplementation(libs.edc.lib.keys)
    testImplementation(libs.edc.junit)
}
