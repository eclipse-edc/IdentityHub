plugins {
    `java-library`
}

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(project(":spi:identity-hub-store-spi"))
    api(libs.edc.spi.transaction)
    implementation(project(":extensions:common:security"))
    implementation(libs.edc.common.crypto)
    implementation(libs.edc.core.connector)
    testImplementation(libs.edc.junit)
}
