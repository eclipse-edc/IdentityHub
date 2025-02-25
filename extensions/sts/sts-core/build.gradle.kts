plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.spi.transaction)
    api(libs.edc.spi.dcp)
    api(project(":spi:sts-spi"))
    api(libs.edc.spi.jwt.signer)

    implementation(libs.edc.spi.keys)
//    implementation(project(":extensions:common:iam:identity-trust:identity-trust-sts:identity-trust-sts-embedded"))
    implementation(project(":extensions:sts:sts-account-service-local"))
    implementation(libs.edc.lib.token)
    implementation(libs.edc.lib.store)

    testImplementation(testFixtures(project(":spi:sts-spi")))
    testImplementation(libs.edc.lib.boot)
    testImplementation(libs.edc.lib.common.crypto)
    testImplementation(libs.edc.lib.keys)
    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.lib.query)
    testImplementation(libs.nimbus.jwt)
}
