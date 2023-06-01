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

include(":core:identity-hub")
include(":core:identity-hub-client")
include(":core:identity-hub-credentials-verifier")
include(":extensions:credentials:identity-hub-credentials-jwt")
include(":extensions:identity-hub-api")
include(":extensions:identity-hub-verifier-jwt")
include(":extensions:self-description-api")
include(":extensions:store:sql:identity-hub-store-sql")
include(":identity-hub-cli")
include(":launcher")
include(":spi:identity-hub-client-spi")
include(":spi:identity-hub-spi")
include(":spi:identity-hub-store-spi")
include(":system-tests")
include(":version-catalog")
