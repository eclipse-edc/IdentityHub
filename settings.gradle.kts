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
// IdentityHub SPI modules
include(":spi:identity-hub-spi")
include(":spi:participant-context-spi")
include(":spi:verifiable-credential-spi")
include(":spi:keypair-spi")
include(":spi:did-spi")

// IssuerService SPI modules
include(":spi:issuerservice:issuerservice-participant-spi")
include(":spi:issuerservice:issuerservice-credential-spi")
include(":spi:issuerservice:credential-revocation-spi")
include(":spi:issuerservice:issuerservice-issuance-spi")

// IdentityHub core modules
include(":core:identity-hub-core")
include(":core:identity-hub-participants")
include(":core:identity-hub-keypairs")
include(":core:identity-hub-did")

// IssuerService core modules
include(":core:issuerservice:issuerservice-core")
include(":core:issuerservice:issuerservice-participants")
include(":core:issuerservice:issuerservice-credentials")
include(":core:issuerservice:issuerservice-credential-revocation")
include(":core:issuerservice:issuerservice-issuance")

// lib modules
include(":core:lib:keypair-lib")
include(":core:lib:accesstoken-lib")
include(":core:lib:common-lib")

// extension modules
include(":extensions:store:sql:identity-hub-did-store-sql")
include(":extensions:store:sql:identity-hub-credentials-store-sql")
include(":extensions:store:sql:identity-hub-participantcontext-store-sql")
include(":extensions:store:sql:identity-hub-keypair-store-sql")
include(":extensions:store:sql:issuerservice-participant-store-sql")
include(":extensions:store:sql:issuerservice-credential-definition-store-sql")
include(":extensions:store:sql:issuerservice-attestation-definition-store-sql")
include(":extensions:store:sql:issuance-process-store-sql")
include(":extensions:did:local-did-publisher")
include(":extensions:common:credential-watchdog")
include(":extensions:sts:sts-account-provisioner")
include(":extensions:sts:sts-account-service-local")
include(":extensions:sts:sts-account-service-remote")

// DCP protocol modules
include(":protocols:dcp:dcp-spi")
include(":protocols:dcp:dcp-issuer:dcp-issuer-spi")
include(":protocols:dcp:dcp-transform-lib")
include(":protocols:dcp:dcp-issuer:dcp-issuer-api")
include(":protocols:dcp:dcp-issuer:dcp-issuer-core")

include(":protocols:dcp:dcp-identityhub:presentation-api")
include(":protocols:dcp:dcp-identityhub:storage-api")


// Identity APIs
include(":extensions:api:identity-api:api-configuration")
include(":extensions:api:identityhub-api-authentication")
include(":extensions:api:identityhub-api-authorization")
include(":extensions:api:identity-api:participant-context-api")
include(":extensions:api:identity-api:verifiable-credentials-api")
include(":extensions:api:identity-api:did-api")
include(":extensions:api:identity-api:keypair-api")

// Issuer Admin API
include(":extensions:api:issuer-admin-api:issuer-admin-api-configuration")
include(":extensions:api:issuer-admin-api:participant-api")
include(":extensions:api:issuer-admin-api:credentials-api")
include(":extensions:api:issuer-admin-api:attestation-api")
include(":extensions:api:issuer-admin-api:credential-definition-api")

// Identity API validators
include(":extensions:api:identity-api:validators:keypair-validators")
include(":extensions:api:identity-api:validators:participant-context-validators")
include(":extensions:api:identity-api:validators:verifiable-credential-validators")

// issuance modules
include(":extensions:issuance:issuerservice-issuance-attestations")
include(":extensions:issuance:issuerservice-issuance-rules")

// other modules
include(":launcher:identityhub")
include(":launcher:issuer-service")

include(":version-catalog")

// test modules
include(":e2e-tests")
include(":e2e-tests:identity-api-tests")
include(":e2e-tests:sts-tests")
include(":e2e-tests:runtimes:identityhub-remote-sts")
include(":e2e-tests:runtimes:sts")
include(":e2e-tests:bom-tests")
include(":e2e-tests:fixtures")
include(":e2e-tests:admin-api-tests")

// BOM modules
include(":dist:bom:identityhub-base-bom")
include(":dist:bom:identityhub-bom")
include(":dist:bom:identityhub-with-sts-bom")
include(":dist:bom:identityhub-feature-sql-bom")
include(":dist:bom:identityhub-feature-sql-sts-bom")
include(":dist:bom:issuerservice-base-bom")
include(":dist:bom:issuerservice-bom")
include(":dist:bom:issuerservice-feature-sql-bom")