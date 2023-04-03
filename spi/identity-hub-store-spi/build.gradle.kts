plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
}

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(libs.edc.spi.core)

    testFixturesImplementation(testFixtures(project(":spi:identity-hub-spi")))
    testFixturesImplementation(root.junit.jupiter.api)
    testFixturesImplementation(root.assertj)
    testFixturesImplementation(root.nimbus.jwt)
    testFixturesImplementation(root.jackson.databind)
}
