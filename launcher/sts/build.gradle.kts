plugins {
    `java-library`
}


dependencies {
    // required modules
    runtimeOnly(libs.bundles.connector)
    runtimeOnly(libs.edc.sts.spi)
    runtimeOnly(libs.edc.sts.api)
    runtimeOnly(libs.edc.sts.core)

    // optional modules
    runtimeOnly(libs.edc.api.version)
}

edcBuild {
    publish.set(false)
}