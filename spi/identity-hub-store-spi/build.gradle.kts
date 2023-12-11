plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
}

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(libs.edc.spi.core)

    testFixturesImplementation(testFixtures(project(":spi:identity-hub-spi")))
    testFixturesImplementation(libs.edc.junit)
    testFixturesImplementation(libs.junit.jupiter.api)
    testFixturesImplementation(libs.assertj)
    testFixturesImplementation(libs.nimbus.jwt)
    testFixturesImplementation(libs.jackson.databind)
}
