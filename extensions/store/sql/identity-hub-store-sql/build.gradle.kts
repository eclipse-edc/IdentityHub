plugins {
    `java-library`
}

dependencies {
    api(project(":spi:identity-hub-store-spi"))
    implementation(libs.edc.spi.transaction)
    implementation(libs.edc.spi.transaction.datasource)
    implementation(libs.edc.core.sql)

    testImplementation(libs.edc.core.junit)
    testImplementation(testFixtures(libs.edc.core.sql))
    testImplementation(testFixtures(project(":spi:identity-hub-store-spi")))
    testImplementation(root.postgres)
}
