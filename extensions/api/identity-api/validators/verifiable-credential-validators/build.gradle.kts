plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.spi.validator)
    api(project(":spi:verifiable-credential-spi"))
    implementation(libs.edc.lib.util)

    testImplementation(libs.edc.junit)
}
