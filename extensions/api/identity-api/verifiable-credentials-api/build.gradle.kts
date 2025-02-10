plugins {
    `java-library`
    `maven-publish`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    api(project(":spi:verifiable-credential-spi"))
    implementation(project(":extensions:api:identity-api:api-configuration"))
    implementation(project(":extensions:api:identity-api:validators:verifiable-credential-validators"))
    implementation(libs.edc.spi.web)
    implementation(libs.edc.lib.util) // StringUtils
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