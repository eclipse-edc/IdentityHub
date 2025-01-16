plugins {
    `java-library`
    `maven-publish`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    api(libs.edc.spi.core)
    api(project(":spi:identity-hub-spi"))
    api(project(":spi:keypair-spi"))
    implementation(project(":extensions:api:identity-api:api-configuration"))
    implementation(project(":extensions:api:identity-api:validators:keypair-validators"))
    implementation(libs.edc.spi.web)
    implementation(libs.edc.lib.util)
    implementation(libs.jakarta.rsApi)
    implementation(libs.jakarta.annotation)

    testImplementation(libs.edc.junit)
    testImplementation(libs.restAssured)
    testImplementation(testFixtures(libs.edc.core.jersey))
}

edcBuild {
    swagger {
        apiGroup.set("identity-api")
    }
}