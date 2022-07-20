plugins {
    `java-library`
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("identity-hub-store-spi") {
            artifactId = "identity-hub-client-store-spi"
            from(components["java"])
        }
    }
}