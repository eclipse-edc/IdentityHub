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
include(":spi:participant-context-spi")
include(":spi:verifiable-credential-spi")
include(":spi:keypair-spi")
include(":spi:identity-hub-store-spi")
include(":spi:identity-hub-did-spi")

// core modules
include(":core:presentation-api")
include(":core:identity-hub-credentials")
include(":core:identity-hub-participants")
include(":core:identity-hub-keypairs")
include(":core:identity-hub-did")

// lib modules
include(":core:lib:verifiable-presentation-lib")
include(":core:lib:keypair-lib")
include(":core:lib:accesstoken-lib")
include(":core:lib:credential-query-lib")

// extension modules
include(":extensions:store:sql:identity-hub-did-store-sql")
include(":extensions:store:sql:identity-hub-credentials-store-sql")
include(":extensions:store:sql:identity-hub-participantcontext-store-sql")
include(":extensions:store:sql:identity-hub-keypair-store-sql")
include(":extensions:did:local-did-publisher")

// management APIs
include(":extensions:api:management-api:validators")
include(":extensions:api:management-api:api-configuration")
include(":extensions:api:identityhub-api-authentication")
include(":extensions:api:identityhub-api-authorization")
include(":extensions:api:management-api:participant-context-api")
include(":extensions:api:management-api:verifiable-credentials-api")
include(":extensions:api:management-api:did-api")
include(":extensions:api:management-api:keypair-api")

// other modules
include(":launcher")

include(":version-catalog")
// test modules
include(":e2e-tests")
include(":e2e-tests:api-tests")
