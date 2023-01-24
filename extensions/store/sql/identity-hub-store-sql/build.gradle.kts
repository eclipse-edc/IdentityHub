plugins {
    `java-library`
}

dependencies {
    api(project(":spi:identity-hub-store-spi"))
    implementation(edc.spi.transaction)
    implementation(edc.spi.transaction.datasource)
    implementation(edc.core.sql)

    testImplementation(edc.core.junit)
    testImplementation(testFixtures(edc.core.sql))
    testImplementation(testFixtures(project(":spi:identity-hub-store-spi")))
    testImplementation(libs.postgres)
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}
