plugins {
    `java-library`
}

dependencies {
    api(project(":spi:issuerservice:issuerservice-holder-spi"))
    api(project(":spi:issuerservice:issuerservice-issuance-spi"))
    api(project(":core:lib:common-lib"))
    api(project(":core:lib:common-lib"))

    implementation(project(":extensions:issuance:local-statuslist-publisher"))
    implementation(libs.edc.lib.store)
    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.lib.query)
    testImplementation(testFixtures(project(":spi:issuerservice:issuerservice-holder-spi")))
    testImplementation(testFixtures(project(":spi:issuerservice:issuerservice-issuance-spi")))

}
