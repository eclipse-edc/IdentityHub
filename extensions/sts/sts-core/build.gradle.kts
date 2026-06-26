plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.spi.core)
    api(libs.edc.spi.dcp)
    api(project(":spi:sts-spi"))

    implementation(project(":extensions:sts:sts-account-service-local"))
    implementation(libs.edc.lib.core)
    implementation(libs.edc.lib.controlplane)

    testImplementation(testFixtures(project(":spi:sts-spi")))
    testImplementation(libs.edc.junit)
    testImplementation(libs.nimbus.jwt)
}
