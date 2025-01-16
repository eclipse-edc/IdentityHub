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
include(":spi:did-spi")

// core modules

include(":core:identity-hub-core")
include(":core:identity-hub-participants")
include(":core:identity-hub-keypairs")
include(":core:identity-hub-did")

// lib modules
include(":core:lib:verifiable-presentation-lib")
include(":core:lib:keypair-lib")
include(":core:lib:accesstoken-lib")

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

// DCP protocol modules
include(":extensions:protocols:dcp:presentation-api")
include(":extensions:protocols:dcp:credential-request-api")
include(":extensions:protocols:dcp:issuer-metadata-api")
include(":extensions:protocols:dcp:credential-request-status-api")

// Identity APIs
include(":extensions:api:identity-api:validators")
include(":extensions:api:identity-api:api-configuration")
include(":extensions:api:identityhub-api-authentication")
include(":extensions:api:identityhub-api-authorization")
include(":extensions:api:identity-api:participant-context-api")
include(":extensions:api:identity-api:verifiable-credentials-api")
include(":extensions:api:identity-api:did-api")
include(":extensions:api:identity-api:keypair-api")
// Identity API validators
include(":extensions:api:identity-api:validators:keypair-validators")
include(":extensions:api:identity-api:validators:participant-context-validators")
include(":extensions:api:identity-api:validators:verifiable-credential-validators")

// other modules
include(":launcher:identityhub")

include(":version-catalog")
// test modules
include(":e2e-tests")
include(":e2e-tests:api-tests")
include(":e2e-tests:sts-tests")
include(":e2e-tests:runtimes:identityhub-remote-sts")
include(":e2e-tests:runtimes:sts")
include(":e2e-tests:bom-tests")

// BOM modules
include(":dist:bom:identityhub-base-bom")
include(":dist:bom:identityhub-bom")
include(":dist:bom:identityhub-with-sts-bom")
include(":dist:bom:identityhub-feature-sql-bom")
include(":dist:bom:identityhub-feature-sql-sts-bom")
include(":dist:bom:issuerservice-base-bom")
include(":dist:bom:issuerservice-bom")
include(":dist:bom:issuerservice-feature-sql-bom")