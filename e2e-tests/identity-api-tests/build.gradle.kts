plugins {
    `java-library`
}


dependencies {
    testImplementation(project(":spi:identity-hub-spi"))
    testImplementation(project(":core:identity-hub-participants"))
    testImplementation(project(":spi:holder-credential-request-spi"))
    testImplementation(libs.edc.junit)
    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
    testImplementation(libs.wiremock)

    // needed for the Participant
    testImplementation(testFixtures(project(":spi:verifiable-credential-spi")))
    testImplementation(testFixtures(libs.edc.testfixtures.managementapi))
    testImplementation(libs.nimbus.jwt)
    testImplementation(project(":protocols:dcp:dcp-spi"))

    testImplementation(project(":spi:sts-spi"))
    testImplementation(testFixtures(project(":e2e-tests:identityhub-test-fixtures")))
    testImplementation(testFixtures(libs.edc.lib.oauth2.authn))

    testCompileOnly(project(":dist:bom:identityhub-bom"))
    testCompileOnly(project(":dist:bom:identityhub-feature-sql-bom"))

    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.vault)
    testRuntimeOnly(libs.bouncyCastle.bcpkixJdk18on)
}

edcBuild {
    publish.set(false)
}
