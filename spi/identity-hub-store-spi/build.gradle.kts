plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
}

dependencies {
    api(project(":spi:identity-hub-spi"))
    api(edc.spi.core)
    

    testFixturesImplementation(libs.junit.jupiter.api)
    testFixturesImplementation(libs.assertj)
    testFixturesImplementation(libs.nimbus.jwt)
    testFixturesImplementation(libs.jackson.databind)
}

publishing {
    publications {
        create<MavenPublication>("identity-hub-store-spi") {
            artifactId = "identity-hub-store-spi"
            from(components["java"])
        }
    }
}
