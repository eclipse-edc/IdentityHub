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
include(":spi:did-spi")

// core modules
include(":core:presentation-api")
include(":core:identity-hub-core")
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
include(":extensions:common:credential-watchdog")
include(":extensions:sts:sts-account-provisioner")
include(":extensions:sts:sts-account-service-local")
include(":extensions:sts:sts-account-service-remote")

// Identity APIs
include(":extensions:api:identity-api:validators")
include(":extensions:api:identity-api:api-configuration")
include(":extensions:api:identityhub-api-authentication")
include(":extensions:api:identityhub-api-authorization")
include(":extensions:api:identity-api:participant-context-api")
include(":extensions:api:identity-api:verifiable-credentials-api")
include(":extensions:api:identity-api:did-api")
include(":extensions:api:identity-api:keypair-api")

// other modules
include(":launcher:identityhub")

include(":version-catalog")
// test modules
include(":e2e-tests")
include(":e2e-tests:api-tests")
include(":e2e-tests:sts-tests")
include(":e2e-tests:runtimes:identityhub-remote-sts")
include(":e2e-tests:runtimes:sts")
