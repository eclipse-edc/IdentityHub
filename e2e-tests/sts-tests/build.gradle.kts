plugins {
    `java-library`
}


dependencies {

    testImplementation(project(":spi:participant-context-spi"))
    testImplementation(libs.edc.sts.spi)

    testImplementation(libs.edc.junit)
    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
}

edcBuild {
    publish.set(false)
}