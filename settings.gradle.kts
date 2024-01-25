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
include(":spi:identity-hub-did-spi")

// core modules
include(":core:identity-hub-api")
include(":core:identity-hub-credentials")
include(":core:identity-hub-participants")
include(":core:identity-hub-keypairs")
include(":core:identity-hub-did")

// extension modules
include(":extensions:common:security")
include(":extensions:store:sql:identity-hub-did-store-sql")
include(":extensions:store:sql:identity-hub-credentials-store-sql")
include(":extensions:store:sql:identity-hub-participantcontext-store-sql")
include(":extensions:store:sql:identity-hub-keypair-store-sql")
include(":extensions:did:local-did-publisher")

// management APIs
include(":extensions:api:identityhub-management-api-configuration")
include(":extensions:api:identityhub-api-auth")
include(":extensions:api:participant-context-mgmt-api")
include(":extensions:api:verifiable-credential-mgmt-api")
include(":extensions:api:keypair-mgmt-api")

include(":extensions:did:did-management-api")
// other modules
include(":launcher")

include(":version-catalog")
// test modules
include(":e2e-tests")
include(":e2e-tests:api-tests")
