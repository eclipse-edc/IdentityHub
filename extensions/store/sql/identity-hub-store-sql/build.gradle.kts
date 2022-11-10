plugins {
    `java-library`
}

val edcVersion: String by project
val edcGroup: String by project
val assertj: String by project
val jupiterVersion: String by project
val mockitoVersion: String by project
val postgresVersion: String by project


dependencies {
    api(project(":spi:identity-hub-store-spi"))
    implementation("${edcGroup}:transaction-spi:${edcVersion}")
    implementation("${edcGroup}:transaction-datasource-spi:${edcVersion}")
    implementation("${edcGroup}:sql-core:${edcVersion}")

    testImplementation("org.assertj:assertj-core:${assertj}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${jupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
    testImplementation("${edcGroup}:junit:${edcVersion}")
    testImplementation("${edcGroup}:sql-core:${edcVersion}:test-fixtures")
    testImplementation(testFixtures(project(":spi:identity-hub-store-spi")))
    testImplementation("org.postgresql:postgresql:${postgresVersion}")
}

publishing {
    publications {
        create<MavenPublication>("identity-hub-store-sql") {
            artifactId = "identity-hub-store-sql"
            from(components["java"])
        }
    }
}
