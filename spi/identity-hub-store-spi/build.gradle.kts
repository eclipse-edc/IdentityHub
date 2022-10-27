plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
}

val edcGroup: String by project
val edcVersion: String by project
val jupiterVersion: String by project
val assertj: String by project
val nimbusVersion: String by project
val jacksonVersion: String by project

dependencies {
    api(project(":spi:identity-hub-spi"))
    api("${edcGroup}:core-spi:${edcVersion}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testImplementation("org.assertj:assertj-core:${assertj}")

    testFixturesImplementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testFixturesImplementation("org.assertj:assertj-core:${assertj}")
    testFixturesImplementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
}

publishing {
    publications {
        create<MavenPublication>("identity-hub-store-spi") {
            artifactId = "identity-hub-store-spi"
            from(components["java"])
        }
    }
}