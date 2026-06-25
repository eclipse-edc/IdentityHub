plugins {
    `java-library`
}

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(project(":spi:verifiable-credential-spi"))
    api(project(":spi:keypair-spi"))
    api(project(":spi:participant-context-spi"))
    api(project(":spi:did-spi"))
    api(project(":spi:holder-credential-request-spi"))
    api(project(":protocols:dcp:dcp-spi"))
    implementation(project(":core:lib:accesstoken-lib"))
    implementation(project(":core:lib:common-lib"))
    implementation(libs.edc.spi.dcp) //SignatureSuiteRegistry
    implementation(libs.edc.spi.core)
    implementation(libs.edc.jsonld) // for the JSON-LD mapper
    implementation(libs.edc.lib.core)
    implementation(libs.edc.lib.controlplane)
    implementation(libs.edc.lib.jsonld)
    implementation(libs.edc.lib.jws2020)
    implementation(libs.edc.vc.ldp)
    implementation(libs.edc.vc.jwt)


    testImplementation(libs.edc.junit)
    testImplementation(testFixtures(project(":spi:keypair-spi")))
    testImplementation(testFixtures(project(":spi:verifiable-credential-spi")))
    testImplementation(testFixtures(project(":spi:holder-credential-request-spi")))
    testImplementation(testFixtures(libs.edc.vc.jwt)) // JWT generator

}
