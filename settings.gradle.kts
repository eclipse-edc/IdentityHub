rootProject.name = "identity-hub"

// this is needed to have access to snapshot builds of plugins
pluginManagement {
    repositories {
        mavenLocal()
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {

    repositories {
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        mavenCentral()
        mavenLocal()
    }
}
// SPI modules
include(":spi:identity-hub-spi")
include(":spi:identity-hub-store-spi")

// core modules
include(":core:identity-hub-api")
include(":core:identity-hub-transform")
include(":core:identity-hub-core")

// extension modules
include(":extensions:cryptography:public-key-provider")

// other modules
include(":launcher")
include(":version-catalog")

// test modules
include(":e2e-tests")
include(":e2e-tests:api-tests")
