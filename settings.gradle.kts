rootProject.name = "identity-hub"

pluginManagement {
    repositories {
        maven {
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// IdentityHub SPI modules
include(":spi:identity-hub-spi")
include(":spi:participant-context-spi")
include(":spi:verifiable-credential-spi")
include(":spi:keypair-spi")
include(":spi:did-spi")
include(":spi:holder-credential-request-spi")
include(":spi:sts-spi")

// IssuerService SPI modules
include(":spi:issuerservice:issuerservice-holder-spi")
include(":spi:issuerservice:issuerservice-credential-spi")
include(":spi:issuerservice:credential-revocation-spi")
include(":spi:issuerservice:issuerservice-issuance-spi")

// IdentityHub core modules
include(":core:common-core")
include(":core:identity-hub-core")
include(":core:identity-hub-participants")
include(":core:identity-hub-keypairs")
include(":core:identity-hub-did")

// IssuerService core modules
include(":core:issuerservice:issuerservice-core")
include(":core:issuerservice:issuerservice-holders")
include(":core:issuerservice:issuerservice-credentials")
include(":core:issuerservice:issuerservice-credential-revocation")
include(":core:issuerservice:issuerservice-issuance")

// lib modules
include(":core:lib:keypair-lib")
include(":core:lib:accesstoken-lib")
include(":core:lib:common-lib")
include(":core:lib:issuerservice-common-lib")

// extension modules
include(":extensions:store:sql:identity-hub-did-store-sql")
include(":extensions:store:sql:identity-hub-credentials-store-sql")
include(":extensions:store:sql:identity-hub-participantcontext-store-sql")
include(":extensions:store:sql:identity-hub-keypair-store-sql")
include(":extensions:store:sql:holder-credential-request-store-sql")
include(":extensions:store:sql:holder-credential-offer-store-sql")
include(":extensions:store:sql:issuerservice-holder-store-sql")
include(":extensions:store:sql:issuerservice-credential-definition-store-sql")
include(":extensions:store:sql:issuerservice-attestation-definition-store-sql")
include(":extensions:store:sql:issuance-process-store-sql")
include(":extensions:store:sql:sts-client-store-sql")
include(":extensions:did:local-did-publisher")
include(":extensions:credentials:credential-offer-handler")
include(":extensions:common:credential-watchdog")
include(":extensions:sts:sts-account-provisioner")
include(":extensions:sts:sts-account-service-local")
include(":extensions:sts:sts-core")
include(":extensions:sts:sts-api")

// DCP protocol modules
include(":protocols:dcp:dcp-spi")
include(":protocols:dcp:dcp-core")
include(":protocols:dcp:dcp-issuer:dcp-issuer-spi")
include(":protocols:dcp:dcp-transform-lib")
include(":protocols:dcp:dcp-validation-lib")
include(":protocols:dcp:dcp-issuer:dcp-issuer-api")
include(":protocols:dcp:dcp-issuer:dcp-issuer-core")

include(":protocols:dcp:dcp-identityhub:credentials-api-configuration")
include(":protocols:dcp:dcp-identityhub:presentation-api")
include(":protocols:dcp:dcp-identityhub:storage-api")
include(":protocols:dcp:dcp-identityhub:credential-offer-api")
include(":protocols:dcp:dcp-identityhub:dcp-identityhub-transform-lib")
include(":protocols:dcp:dcp-identityhub:dcp-identityhub-core")

// Identity APIs
include(":extensions:api:identity-api")
include(":extensions:api:identity-api:api-configuration")
include(":extensions:api:identityhub-api-authentication")
include(":extensions:api:lib:identityhub-api-authentication-lib")
include(":extensions:api:identityhub-api-authorization")
include(":extensions:api:identity-api:participant-context-api")
include(":extensions:api:identity-api:verifiable-credentials-api")
include(":extensions:api:identity-api:did-api")
include(":extensions:api:identity-api:keypair-api")

// Issuer Admin API
include(":extensions:api:issuer-admin-api:issuer-admin-api-configuration")
include(":extensions:api:issuer-admin-api:holder-api")
include(":extensions:api:issuer-admin-api:credentials-api")
include(":extensions:api:issuer-admin-api:attestation-api")
include(":extensions:api:issuer-admin-api:credential-definition-api")
include(":extensions:api:issuer-admin-api:issuance-process-api")
include(":extensions:api:issuer-admin-api:issuer-admin-api-authentication")

// Identity API validators
include(":extensions:api:identity-api:validators:keypair-validators")
include(":extensions:api:identity-api:validators:participant-context-validators")
include(":extensions:api:identity-api:validators:verifiable-credential-validators")

// issuance modules
include(":extensions:issuance:issuerservice-presentation-attestations")
include(":extensions:issuance:issuerservice-database-attestations")
include(":extensions:issuance:issuerservice-issuance-rules")
include(":extensions:issuance:local-statuslist-publisher")

// other modules
include(":launcher:identityhub")
include(":launcher:issuer-service")

include(":version-catalog")

// test modules
include(":e2e-tests")
include(":e2e-tests:identity-api-tests")
include(":e2e-tests:runtimes:sts")
include(":e2e-tests:bom-tests")
include(":e2e-tests:identityhub-test-fixtures")
include(":e2e-tests:admin-api-tests")
include(":e2e-tests:dcp-issuance-tests")
include(":e2e-tests:sts-api-tests")
include(":e2e-tests:tck-tests:presentation")
include(":e2e-tests:tck-tests:test-attestations")

// BOM modules
include(":dist:bom:identityhub-base-bom")
include(":dist:bom:identityhub-bom")
include(":dist:bom:identityhub-feature-sql-bom")
include(":dist:bom:issuerservice-base-bom")
include(":dist:bom:issuerservice-bom")
include(":dist:bom:issuerservice-feature-sql-bom")
