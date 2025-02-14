plugins {
    `java-library`
}


dependencies {
    testImplementation(project(":spi:identity-hub-spi"))
    testImplementation(project(":core:identity-hub-participants"))
    testImplementation(libs.edc.junit)
    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)


    // needed for the Participant
    testImplementation(testFixtures(project(":spi:verifiable-credential-spi")))
    testImplementation(testFixtures(libs.edc.testfixtures.managementapi))
    testImplementation(libs.nimbus.jwt)
    testImplementation(project(":protocols:dcp:dcp-spi"))

    testImplementation(libs.edc.sts.spi)
    testImplementation(testFixtures(project(":e2e-tests:fixtures")))

    testCompileOnly(project(":dist:bom:identityhub-with-sts-bom"))
    testCompileOnly(project(":dist:bom:identityhub-feature-sql-bom"))
}

edcBuild {
    publish.set(false)
}
