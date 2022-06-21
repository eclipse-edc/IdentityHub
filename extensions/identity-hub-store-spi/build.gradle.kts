plugins {
    `java-library`
}

val edcVersion: String by project
val edcGroup: String by project
val jupiterVersion: String by project

dependencies {
    implementation("${edcGroup}:core:${edcVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}")
}