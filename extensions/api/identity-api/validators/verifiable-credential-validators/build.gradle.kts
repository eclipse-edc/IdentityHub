plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.spi.core)
    api(project(":spi:verifiable-credential-spi"))
    implementation(libs.edc.lib.core)

    testImplementation(libs.edc.junit)
}
