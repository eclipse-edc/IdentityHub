plugins {
    `java-library`
}


dependencies {
    testImplementation(project(":spi:identity-hub-spi"))
    testImplementation(project(":core:identity-hub-participants"))
    testImplementation(libs.edc.junit)
    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgres)

    // needed for the Participant
    testImplementation(testFixtures(project(":spi:verifiable-credential-spi")))
    testImplementation(testFixtures(libs.edc.testfixtures.managementapi))
    testImplementation(testFixtures(libs.edc.sql.test.fixtures))
    testImplementation(libs.edc.transaction.local)
    testImplementation(libs.nimbus.jwt)
    testImplementation(libs.jakarta.rsApi)
    testImplementation(libs.edc.sts.spi)

    testCompileOnly(project(":dist:bom:identityhub-with-sts-bom"))
    testCompileOnly(project(":dist:bom:identityhub-feature-sql-bom"))
}

edcBuild {
    publish.set(false)
}
