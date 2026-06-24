plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.spi.core)
    api(libs.edc.spi.dcp)
    api(project(":spi:sts-spi"))

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
