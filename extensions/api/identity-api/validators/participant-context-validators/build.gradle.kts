plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":extensions:api:identity-api:validators:keypair-validators"))
    implementation(libs.edc.lib.core)

    testImplementation(libs.edc.junit)
}
