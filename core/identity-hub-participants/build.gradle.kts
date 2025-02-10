plugins {
    `java-library`
}

dependencies {
    api(project(":spi:did-spi"))
    api(project(":spi:participant-context-spi"))
    implementation(project(":spi:keypair-spi"))
    api(libs.edc.spi.transaction)
    runtimeOnly(libs.bouncyCastle.bcprovJdk18on)
    
    testImplementation(libs.edc.lib.keys)
    testImplementation(libs.edc.junit)
}
