plugins {
    `java-library`
}

dependencies {
    api(project(":spi:issuance-credentials-spi"))
    implementation(project(":core:lib:common-lib"))
    implementation(libs.edc.spi.transaction)
    implementation(libs.edc.lib.store)
    implementation(libs.edc.lib.statemachine)
    testImplementation(libs.edc.junit)
}
