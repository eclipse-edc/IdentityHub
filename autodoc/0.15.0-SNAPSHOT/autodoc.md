Module `attestation-api`
------------------------
**Artifact:** org.eclipse.edc:attestation-api:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.issuerservice.api.admin.credentials.IssuerAttestationAdminApiExtension`
**Name:** "Issuer Service Credentials Admin API Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionService` (required)
- `org.eclipse.edc.identityhub.spi.authorization.AuthorizationService` (required)

Module `common-core`
--------------------
**Artifact:** org.eclipse.edc:common-core:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.DefaultServicesExtension`
**Name:** "IdentityHub Default Services Extension"

**Overview:** No overview provided.


### Configuration

| Key                                            | Required | Type     | Default  | Pattern | Min | Max | Description                                                                                  |
| ---------------------------------------------- | -------- | -------- | -------- | ------- | --- | --- | -------------------------------------------------------------------------------------------- |
| `edc.iam.accesstoken.jti.validation`           | `*`      | `string` | `false`  |         |     |     | Activates the JTI check: access tokens can only be used once to guard against replay attacks |
| `edc.iam.credential.revocation.cache.validity` | `*`      | `string` | `900000` |         |     |     | Validity period of cached StatusList2021 credential entries in milliseconds.                 |
| `edc.iam.credential.revocation.mimetype`       | `*`      | `string` | `*`      |         |     |     | A comma-separated list of accepted content types of the revocation list credential.          |

#### Provided services
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore`
- `org.eclipse.edc.identityhub.spi.participantcontext.store.ParticipantContextStore`
- `org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore`
- `org.eclipse.edc.identityhub.spi.transformation.ScopeToCriterionTransformer`
- `org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry`
- `org.eclipse.edc.iam.identitytrust.spi.verification.SignatureSuiteRegistry`
- `org.eclipse.edc.jwt.signer.spi.JwsSignerProvider`
- `org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore`
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore`

#### Referenced (injected) services
- `org.eclipse.edc.token.spi.TokenValidationRulesRegistry` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.keys.spi.PrivateKeyResolver` (required)
- `org.eclipse.edc.jwt.validation.jti.JtiValidationStore` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.query.CriterionOperatorRegistry` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.jwt.validation.jti.JtiValidationStore` (required)
- `org.eclipse.edc.http.spi.EdcHttpClient` (required)

Module `credential-definition-api`
----------------------------------
**Artifact:** org.eclipse.edc:credential-definition-api:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.issuerservice.api.admin.credentialdefinition.IssuerCredentialDefinitionAdminApiExtension`
**Name:** "Issuer Service Credential Definition Admin API Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService` (required)
- `org.eclipse.edc.identityhub.spi.authorization.AuthorizationService` (required)

Module `credential-offer-api`
-----------------------------
**Artifact:** org.eclipse.edc:credential-offer-api:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.CredentialOfferApiExtension`
**Name:** "Storage API Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry` (required)
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.CredentialWriter` (required)
- `org.eclipse.edc.identityhub.protocols.dcp.spi.DcpIssuerTokenVerifier` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.offer.CredentialOfferService` (required)

Module `credential-offer-handler`
---------------------------------
**Artifact:** org.eclipse.edc:credential-offer-handler:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.credential.offer.handler.CredentialOfferHandlerExtension`
**Name:** "CredentialOfferHandlerExtension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `org.eclipse.edc.identityhub.protocols.dcp.spi.DcpProfileRegistry` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestManager` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)

Module `credential-watchdog`
----------------------------
**Artifact:** org.eclipse.edc:credential-watchdog:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.common.credentialwatchdog.CredentialWatchdogExtension`
**Name:** "VerifiableCredential Watchdog Extension"

**Overview:** No overview provided.


### Configuration

| Key                                      | Required | Type     | Default  | Pattern | Min | Max | Description                                                                                                                                      |
| ---------------------------------------- | -------- | -------- | -------- | ------- | --- | --- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| `edc.iam.credential.status.check.period` | `*`      | `string` | `60`     |         |     |     | Period (in seconds) at which the Watchdog thread checks all stored credentials for their status. Configuring a number <=0 disables the Watchdog. |
| `edc.iam.credential.status.check.delay`  |          | `string` | ``       |         |     |     | Initial delay (in seconds) before the Watchdog thread begins its work.                                                                           |
| `edc.iam.credential.renewal.graceperiod` | `*`      | `string` | `604800` |         |     |     | Grace period (in seconds) before credential expiry at which automatic renewal is triggered                                                       |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.spi.system.ExecutorInstrumentation` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialStatusCheckService` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestManager` (required)

Module `credentials-api`
------------------------
**Artifact:** org.eclipse.edc:credentials-api:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.issuerservice.api.admin.credentials.IssuerCredentialsAdminApiExtension`
**Name:** "Issuer Service Credentials Admin API Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.issuerservice.spi.credentials.CredentialStatusService` (required)
- `org.eclipse.edc.identityhub.spi.authorization.AuthorizationService` (required)
- `org.eclipse.edc.issuerservice.spi.credentials.IssuerCredentialOfferService` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)

Module `credentials-api-configuration`
--------------------------------------
**Artifact:** org.eclipse.edc:credentials-api-configuration:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.CredentialsApiConfigurationExtension`
**Name:** "Storage API Extension"

**Overview:** No overview provided.


### Configuration

| Key                         | Required | Type     | Default            | Pattern | Min | Max | Description                      |
| --------------------------- | -------- | -------- | ------------------ | ------- | --- | --- | -------------------------------- |
| `web.http.credentials.port` | `*`      | `string` | `13131`            |         |     |     | Port for credentials api context |
| `web.http.credentials.path` | `*`      | `string` | `/api/credentials` |         |     |     | Path for credentials api context |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.spi.system.apiversion.ApiVersionService` (required)
- `org.eclipse.edc.web.spi.configuration.PortMappingRegistry` (required)

Module `dcp-core`
-----------------
**Artifact:** org.eclipse.edc:dcp-core:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.protocols.dcp.DcpCoreExtension`
**Name:** "DCP Core Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.identityhub.protocols.dcp.spi.DcpProfileRegistry`

#### Referenced (injected) services
_None_

Module `dcp-identityhub-core`
-----------------------------
**Artifact:** org.eclipse.edc:dcp-identityhub-core:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.protocols.dcp.issuer.DcpHolderCoreExtension`
**Name:** "DCP Holder Core Extension"

**Overview:** No overview provided.


### Configuration

| Key                                  | Required | Type     | Default | Pattern | Min | Max | Description                                                                                  |
| ------------------------------------ | -------- | -------- | ------- | ------- | --- | --- | -------------------------------------------------------------------------------------------- |
| `edc.iam.accesstoken.jti.validation` | `*`      | `string` | `false` |         |     |     | Activates the JTI check: access tokens can only be used once to guard against replay attacks |

#### Provided services
- `org.eclipse.edc.identityhub.protocols.dcp.spi.DcpIssuerTokenVerifier`

#### Referenced (injected) services
- `java.time.Clock` (required)
- `org.eclipse.edc.token.spi.TokenValidationService` (required)
- `org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver` (required)
- `org.eclipse.edc.jwt.validation.jti.JtiValidationStore` (optional)

Module `dcp-issuer-api`
-----------------------
**Artifact:** org.eclipse.edc:dcp-issuer-api:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.protocols.dcp.issuer.IssuerApiExtension`
**Name:** "Issuer API extension"

**Overview:** No overview provided.


### Configuration

| Key                      | Required | Type     | Default         | Pattern | Min | Max | Description                   |
| ------------------------ | -------- | -------- | --------------- | ------- | --- | --- | ----------------------------- |
| `web.http.issuance.port` | `*`      | `string` | `13132`         |         |     |     | Port for issuance api context |
| `web.http.issuance.path` | `*`      | `string` | `/api/issuance` |         |     |     | Path for issuance api context |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.spi.system.apiversion.ApiVersionService` (required)
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.web.spi.configuration.PortMappingRegistry` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerService` (required)
- `org.eclipse.edc.identityhub.protocols.dcp.spi.DcpHolderTokenVerifier` (required)
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.process.IssuanceProcessService` (required)
- `org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerMetadataService` (required)

Module `dcp-issuer-core`
------------------------
**Artifact:** org.eclipse.edc:dcp-issuer-core:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.protocols.dcp.issuer.DcpIssuerCoreExtension`
**Name:** "DCP Issuer Core Extension"

**Overview:** No overview provided.


### Configuration

| Key                                  | Required | Type     | Default | Pattern | Min | Max | Description                                                                                  |
| ------------------------------------ | -------- | -------- | ------- | ------- | --- | --- | -------------------------------------------------------------------------------------------- |
| `edc.iam.accesstoken.jti.validation` | `*`      | `string` | `false` |         |     |     | Activates the JTI check: access tokens can only be used once to guard against replay attacks |
| `edc.issuance.anonymous.allowed`     | `*`      | `string` | `false` |         |     |     | Allow anonymous onboarding                                                                   |

#### Provided services
- `org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerService`
- `org.eclipse.edc.identityhub.protocols.dcp.spi.DcpHolderTokenVerifier`
- `org.eclipse.edc.issuerservice.spi.issuance.delivery.CredentialStorageClient`
- `org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerMetadataService`
- `org.eclipse.edc.iam.identitytrust.spi.CredentialServiceUrlResolver`

#### Referenced (injected) services
- `org.eclipse.edc.token.spi.TokenValidationRulesRegistry` (required)
- `org.eclipse.edc.token.spi.TokenValidationService` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.store.ParticipantContextStore` (required)
- `org.eclipse.edc.issuerservice.spi.holder.store.HolderStore` (required)
- `org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationPipeline` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleDefinitionEvaluator` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.http.spi.EdcHttpClient` (required)
- `org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry` (required)
- `org.eclipse.edc.identityhub.protocols.dcp.spi.DcpProfileRegistry` (required)
- `org.eclipse.edc.jwt.validation.jti.JtiValidationStore` (optional)

Module `did-api`
----------------
**Artifact:** org.eclipse.edc:did-api:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.didmanagement.DidManagementApiExtension`
**Name:** "DID management Identity API Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.identityhub.spi.did.DidDocumentService` (required)
- `org.eclipse.edc.identityhub.spi.authorization.AuthorizationService` (required)

Module `did-spi`
----------------
**Name:** Identity Hub DID services
**Artifact:** org.eclipse.edc:did-spi:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
  - `org.eclipse.edc.identityhub.spi.did.DidWebParser`
  - `org.eclipse.edc.identityhub.spi.did.store.DidResourceStore`
  - `org.eclipse.edc.identityhub.spi.did.DidDocumentPublisher`

### Extensions
Module `holder-api`
-------------------
**Artifact:** org.eclipse.edc:holder-api:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.issuerservice.api.admin.holder.IssuerHolderAdminApiExtension`
**Name:** "Issuer Service Participant Admin API Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.issuerservice.spi.holder.HolderService` (required)
- `org.eclipse.edc.identityhub.spi.authorization.AuthorizationService` (required)

Module `holder-credential-offer-store-sql`
------------------------------------------
**Artifact:** org.eclipse.edc:holder-credential-offer-store-sql:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.store.sql.credentialoffer.schema.SqlCredentialOfferStoreExtension`
**Name:** "Issuance Process SQL Store Extension"

**Overview:** No overview provided.


### Configuration

| Key                                          | Required | Type     | Default   | Pattern | Min | Max | Description               |
| -------------------------------------------- | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.credentialrequest.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.identityhub.store.sql.credentialoffer.schema.CredentialOfferStoreStatements` (optional)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)
- `org.eclipse.edc.sql.lease.spi.SqlLeaseContextBuilderProvider` (required)
- `java.time.Clock` (required)

Module `holder-credential-request-store-sql`
--------------------------------------------
**Artifact:** org.eclipse.edc:holder-credential-request-store-sql:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.store.sql.credentialrequest.schema.SqlHolderCredentialRequestStoreExtension`
**Name:** "Issuance Process SQL Store Extension"

**Overview:** No overview provided.


### Configuration

| Key                                          | Required | Type     | Default   | Pattern | Min | Max | Description               |
| -------------------------------------------- | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.credentialrequest.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.identityhub.store.sql.credentialrequest.schema.HolderCredentialRequestStoreStatements` (optional)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)
- `org.eclipse.edc.sql.lease.spi.SqlLeaseContextBuilderProvider` (required)
- `java.time.Clock` (required)

Module `identity-api-configuration`
-----------------------------------
**Artifact:** org.eclipse.edc:identity-api-configuration:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.configuration.IdentityApiConfigurationExtension`
**Name:** "Identity API Extension"

**Overview:** No overview provided.


### Configuration

| Key                      | Required | Type     | Default         | Pattern | Min | Max | Description                   |
| ------------------------ | -------- | -------- | --------------- | ------- | --- | --- | ----------------------------- |
| `web.http.identity.port` | `*`      | `string` | `15151`         |         |     |     | Port for identity api context |
| `web.http.identity.path` | `*`      | `string` | `/api/identity` |         |     |     | Path for identity api context |

#### Provided services
- `org.eclipse.edc.identityhub.spi.authorization.AuthorizationService`

#### Referenced (injected) services
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.web.spi.configuration.PortMappingRegistry` (required)
- `org.eclipse.edc.spi.system.apiversion.ApiVersionService` (required)

Module `identity-hub-core`
--------------------------
**Artifact:** org.eclipse.edc:identity-hub-core:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.core.CoreServicesExtension`
**Name:** "IdentityHub Core Services Extension"

**Overview:**  This extension provides core services for the IdentityHub that are not intended to be user-replaceable.



### Configuration_None_

#### Provided services
- `org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenVerifier`
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.resolution.CredentialQueryResolver`
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.PresentationCreatorRegistry`
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.VerifiablePresentationService`
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.CredentialWriter`
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialStatusCheckService`
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestManager`
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.offer.CredentialOfferService`
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.events.CredentialOfferObservable`

#### Referenced (injected) services
- `org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore` (required)
- `org.eclipse.edc.identityhub.spi.transformation.ScopeToCriterionTransformer` (required)
- `org.eclipse.edc.keys.spi.PrivateKeyResolver` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.iam.identitytrust.spi.verification.SignatureSuiteRegistry` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.token.spi.TokenValidationService` (required)
- `org.eclipse.edc.token.spi.TokenValidationRulesRegistry` (required)
- `org.eclipse.edc.spi.security.Vault` (required)
- `org.eclipse.edc.keys.spi.KeyParserRegistry` (required)
- `org.eclipse.edc.iam.identitytrust.spi.verification.SignatureSuiteRegistry` (required)
- `org.eclipse.edc.identityhub.spi.keypair.KeyPairService` (required)
- `org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry` (required)
- `org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore` (required)
- `org.eclipse.edc.keys.spi.LocalPublicKeyService` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService` (required)
- `org.eclipse.edc.jwt.signer.spi.JwsSignerProvider` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry` (required)
- `org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore` (required)
- `org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService` (required)
- `org.eclipse.edc.http.spi.EdcHttpClient` (required)
- `org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore` (required)
- `org.eclipse.edc.spi.event.EventRouter` (required)

Module `identity-hub-credentials-store-sql`
-------------------------------------------
**Artifact:** org.eclipse.edc:identity-hub-credentials-store-sql:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.store.sql.credentials.SqlCredentialStoreExtension`
**Name:** "CredentialResource SQL Store Extension"

**Overview:** No overview provided.


### Configuration

| Key                                    | Required | Type     | Default   | Pattern | Min | Max | Description               |
| -------------------------------------- | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.credentials.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.identityhub.store.sql.credentials.CredentialStoreStatements` (optional)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)

Module `identity-hub-did`
-------------------------
**Artifact:** org.eclipse.edc:identity-hub-did:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.did.DidServicesExtension`
**Name:** "DID Service Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.identityhub.spi.did.DidDocumentPublisherRegistry`
- `org.eclipse.edc.identityhub.spi.did.DidDocumentService`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.identityhub.spi.did.store.DidResourceStore` (required)
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `org.eclipse.edc.keys.spi.KeyParserRegistry` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.store.ParticipantContextStore` (required)

#### Class: `org.eclipse.edc.identityhub.did.defaults.DidDefaultServicesExtension`
**Name:** "DID Default Services Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.identityhub.spi.did.store.DidResourceStore`

#### Referenced (injected) services
- `org.eclipse.edc.spi.query.CriterionOperatorRegistry` (required)

Module `identity-hub-did-store-sql`
-----------------------------------
**Artifact:** org.eclipse.edc:identity-hub-did-store-sql:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.did.store.sql.SqlDidResourceStoreExtension`
**Name:** "DID Resource SQL Store Extension"

**Overview:** No overview provided.


### Configuration

| Key                                    | Required | Type     | Default   | Pattern | Min | Max | Description               |
| -------------------------------------- | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.didresource.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.identityhub.spi.did.store.DidResourceStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.identityhub.did.store.sql.DidResourceStatements` (optional)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)

Module `identity-hub-keypair-store-sql`
---------------------------------------
**Artifact:** org.eclipse.edc:identity-hub-keypair-store-sql:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.store.sql.keypair.SqlKeyPairResourceStoreExtension`
**Name:** "KeyPair Resource SQL Store Extension"

**Overview:** No overview provided.


### Configuration

| Key                                | Required | Type     | Default   | Pattern | Min | Max | Description               |
| ---------------------------------- | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.keypair.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.identityhub.store.sql.keypair.KeyPairResourceStoreStatements` (optional)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)

Module `identity-hub-keypairs`
------------------------------
**Artifact:** org.eclipse.edc:identity-hub-keypairs:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.keypairs.KeyPairServiceExtension`
**Name:** "KeyPair Service Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.identityhub.spi.keypair.KeyPairService`
- `org.eclipse.edc.identityhub.spi.keypair.events.KeyPairObservable`

#### Referenced (injected) services
- `org.eclipse.edc.spi.security.Vault` (required)
- `org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore` (required)
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.store.ParticipantContextStore` (required)

Module `identity-hub-participantcontext-store-sql`
--------------------------------------------------
**Artifact:** org.eclipse.edc:identity-hub-participantcontext-store-sql:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.store.sql.participantcontext.SqlParticipantContextStoreExtension`
**Name:** "ParticipantContext SQL Store Extension"

**Overview:** No overview provided.


### Configuration

| Key                                           | Required | Type     | Default   | Pattern | Min | Max | Description               |
| --------------------------------------------- | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.participantcontext.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.identityhub.spi.participantcontext.store.ParticipantContextStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.identityhub.store.sql.participantcontext.ParticipantContextStoreStatements` (optional)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)

Module `identity-hub-participants`
----------------------------------
**Artifact:** org.eclipse.edc:identity-hub-participants:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.participantcontext.ParticipantContextCoordinatorExtension`
**Name:** "ParticipantContext Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.identityhub.spi.did.DidDocumentService` (required)
- `org.eclipse.edc.identityhub.spi.keypair.KeyPairService` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService` (required)

#### Class: `org.eclipse.edc.identityhub.participantcontext.ParticipantContextExtension`
**Name:** "ParticipantContext Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService`
- `org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextObservable`

#### Referenced (injected) services
- `org.eclipse.edc.identityhub.spi.participantcontext.store.ParticipantContextStore` (required)
- `org.eclipse.edc.spi.security.Vault` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `org.eclipse.edc.identityhub.spi.did.store.DidResourceStore` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.StsAccountProvisioner` (required)

Module `identityhub-api-authentication`
---------------------------------------
**Artifact:** org.eclipse.edc:identityhub-api-authentication:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.ApiAuthenticationExtension`
**Name:** "Identity API Authentication Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService` (required)
- `org.eclipse.edc.spi.security.Vault` (required)

Module `identityhub-api-authorization`
--------------------------------------
**Artifact:** org.eclipse.edc:identityhub-api-authorization:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.ApiAuthorizationExtension`
**Name:** "Identity API Authorization Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.identityhub.spi.authorization.AuthorizationService`

#### Referenced (injected) services
_None_

Module `issuance-process-api`
-----------------------------
**Artifact:** org.eclipse.edc:issuance-process-api:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.issuerservice.api.admin.issuance.IssuanceProcessAdminApiExtension`
**Name:** "Issuer Service Issuance Process Admin API Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.process.IssuanceProcessService` (required)
- `org.eclipse.edc.identityhub.spi.authorization.AuthorizationService` (required)

Module `issuance-process-store-sql`
-----------------------------------
**Artifact:** org.eclipse.edc:issuance-process-store-sql:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.issuerservice.store.sql.issuanceprocess.SqlIssuanceProcessStoreExtension`
**Name:** "Issuance Process SQL Store Extension"

**Overview:** No overview provided.


### Configuration

| Key                                        | Required | Type     | Default   | Pattern | Min | Max | Description               |
| ------------------------------------------ | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.issuanceprocess.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.issuerservice.store.sql.issuanceprocess.IssuanceProcessStoreStatements` (optional)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)
- `org.eclipse.edc.sql.lease.spi.SqlLeaseContextBuilderProvider` (required)
- `java.time.Clock` (required)

Module `issuer-admin-api-authentication`
----------------------------------------
**Artifact:** org.eclipse.edc:issuer-admin-api-authentication:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.IssuerAdminApiAuthenticationExtension`
**Name:** "Issuer Admin API Authentication Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService` (required)
- `org.eclipse.edc.spi.security.Vault` (required)

Module `issuer-admin-api-configuration`
---------------------------------------
**Artifact:** org.eclipse.edc:issuer-admin-api-configuration:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.configuration.IssuerAdminApiConfigurationExtension`
**Name:** "Issuer Admin API Configuration Extension"

**Overview:** No overview provided.


### Configuration

| Key                         | Required | Type     | Default       | Pattern | Min | Max | Description                      |
| --------------------------- | -------- | -------- | ------------- | ------- | --- | --- | -------------------------------- |
| `web.http.issueradmin.port` | `*`      | `string` | `15152`       |         |     |     | Port for issueradmin api context |
| `web.http.issueradmin.path` | `*`      | `string` | `/api/issuer` |         |     |     | Path for issueradmin api context |

#### Provided services
- `org.eclipse.edc.identityhub.spi.authorization.AuthorizationService`

#### Referenced (injected) services
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.web.spi.configuration.PortMappingRegistry` (required)
- `org.eclipse.edc.spi.system.apiversion.ApiVersionService` (required)

Module `issuerservice-attestation-definition-store-sql`
-------------------------------------------------------
**Artifact:** org.eclipse.edc:issuerservice-attestation-definition-store-sql:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.issuerservice.store.sql.attestationdefinition.SqlAttestationDefinitionStoreExtension`
**Name:** "IssuerService Attestation Definition SQL Store Extension"

**Overview:** No overview provided.


### Configuration

| Key                                               | Required | Type     | Default   | Pattern | Min | Max | Description               |
| ------------------------------------------------- | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.attestationdefinitions.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.issuerservice.store.sql.attestationdefinition.AttestationDefinitionStoreStatements` (optional)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)
- `java.time.Clock` (required)

Module `issuerservice-core`
---------------------------
**Artifact:** org.eclipse.edc:issuerservice-core:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.issuerservice.defaults.DefaultServiceExtension`
**Name:** "IssuerService Default Services Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.issuerservice.spi.holder.store.HolderStore`
- `org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionStore`
- `org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.store.CredentialDefinitionStore`
- `org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore`

#### Referenced (injected) services
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.query.CriterionOperatorRegistry` (required)

Module `issuerservice-credential-definition-store-sql`
------------------------------------------------------
**Artifact:** org.eclipse.edc:issuerservice-credential-definition-store-sql:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.issuerservice.store.sql.credentialdefinition.SqlCredentialDefinitionStoreExtension`
**Name:** "IssuerService Credential definition SQL Store Extension"

**Overview:** No overview provided.


### Configuration

| Key                                              | Required | Type     | Default   | Pattern | Min | Max | Description               |
| ------------------------------------------------ | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.credentialdefinitions.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.store.CredentialDefinitionStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.issuerservice.store.sql.credentialdefinition.CredentialDefinitionStoreStatements` (optional)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)
- `java.time.Clock` (required)

Module `issuerservice-credentials`
----------------------------------
**Artifact:** org.eclipse.edc:issuerservice-credentials:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.issuerservice.credentials.CredentialServiceExtension`
**Name:** "Issuer Service Credential Service Extension"

**Overview:** No overview provided.


### Configuration

| Key                                       | Required | Type     | Default | Pattern | Min | Max | Description                                                                    |
| ----------------------------------------- | -------- | -------- | ------- | ------- | --- | --- | ------------------------------------------------------------------------------ |
| `edc.issuer.statuslist.signing.key.alias` | `*`      | `string` | ``      |         |     |     | Alias for the private key that is intended for signing status list credentials |

#### Provided services
- `org.eclipse.edc.issuerservice.spi.credentials.CredentialStatusService`
- `org.eclipse.edc.issuerservice.spi.credentials.IssuerCredentialOfferService`
- `org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListInfoFactoryRegistry`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.jwt.signer.spi.JwsSignerProvider` (required)
- `org.eclipse.edc.issuerservice.spi.holder.store.HolderStore` (required)
- `org.eclipse.edc.iam.identitytrust.spi.CredentialServiceUrlResolver` (required)
- `org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService` (required)
- `org.eclipse.edc.http.spi.EdcHttpClient` (required)
- `org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListManager` (optional)
- `org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService` (required)
- `org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListCredentialPublisher` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerMetadataService` (required)

Module `issuerservice-database-attestations`
--------------------------------------------
**Artifact:** org.eclipse.edc:issuerservice-database-attestations:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.issuerservice.issuance.DatabaseAttestationExtension`
**Name:** "Database Attestations Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactoryRegistry` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionValidatorRegistry` (required)
- `org.eclipse.edc.spi.security.Vault` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)

Module `issuerservice-holder-store-sql`
---------------------------------------
**Artifact:** org.eclipse.edc:issuerservice-holder-store-sql:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.issuerservice.store.sql.holder.SqlHolderStoreExtension`
**Name:** "IssuerService Holder SQL Store Extension"

**Overview:** No overview provided.


### Configuration

| Key                               | Required | Type     | Default   | Pattern | Min | Max | Description               |
| --------------------------------- | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.holder.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.issuerservice.spi.holder.store.HolderStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.issuerservice.store.sql.holder.HolderStoreStatements` (optional)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)

Module `issuerservice-holders`
------------------------------
**Artifact:** org.eclipse.edc:issuerservice-holders:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.issuerservice.holder.HolderServiceExtension`
**Name:** "IssuerService Holder Service Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.issuerservice.spi.holder.HolderService`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.issuerservice.spi.holder.store.HolderStore` (required)

Module `issuerservice-issuance`
-------------------------------
**Artifact:** org.eclipse.edc:issuerservice-issuance:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.issuerservice.issuance.IssuanceServicesExtension`
**Name:** "IssuerService Issuance Services Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService`
- `org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionService`
- `org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationPipeline`
- `org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactoryRegistry`
- `org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleDefinitionEvaluator`
- `org.eclipse.edc.issuerservice.spi.issuance.mapping.IssuanceClaimsMapper`
- `org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleFactoryRegistry`
- `org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleDefinitionValidatorRegistry`
- `org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry`
- `org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionValidatorRegistry`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.store.CredentialDefinitionStore` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionStore` (required)
- `org.eclipse.edc.identityhub.spi.keypair.KeyPairService` (required)
- `org.eclipse.edc.jwt.signer.spi.JwsSignerProvider` (required)
- `org.eclipse.edc.issuerservice.spi.holder.HolderService` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService` (required)

#### Class: `org.eclipse.edc.issuerservice.issuance.IssuanceCoreExtension`
**Name:** "Issuance Core Extension"

**Overview:** No overview provided.


### Configuration

| Key                                   | Required | Type     | Default | Pattern | Min | Max | Description                                                                 |
| ------------------------------------- | -------- | -------- | ------- | ------- | --- | --- | --------------------------------------------------------------------------- |
| `state-machine.iteration-wait-millis` | `*`      | `string` | `1000`  |         |     |     | The iteration wait time in milliseconds in the state machine.               |
| `state-machine.batch-size`            | `*`      | `string` | `20`    |         |     |     | The number of entities to be processed on every iteration.                  |
| `send.retry.limit`                    | `*`      | `string` | `7`     |         |     |     | How many times a specific operation must be tried before failing with error |
| `send.retry.base-delay.ms`            | `*`      | `string` | `1000`  |         |     |     | The base delay for the consumer negotiation retry mechanism in millisecond  |

#### Provided services
- `org.eclipse.edc.issuerservice.spi.issuance.process.IssuanceProcessManager`
- `org.eclipse.edc.issuerservice.spi.issuance.process.IssuanceProcessService`

#### Referenced (injected) services
- `org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.store.CredentialDefinitionStore` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.spi.telemetry.Telemetry` (required)
- `org.eclipse.edc.spi.system.ExecutorInstrumentation` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.process.retry.IssuanceProcessRetryStrategy` (optional)
- `org.eclipse.edc.issuerservice.spi.issuance.delivery.CredentialStorageClient` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.issuerservice.spi.credentials.CredentialStatusService` (required)

Module `issuerservice-issuance-rules`
-------------------------------------
**Artifact:** org.eclipse.edc:issuerservice-issuance-rules:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.issuerservice.issuance.rules.IssuanceRulesExtension`
**Name:** "Issuance Rules Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleFactoryRegistry` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleDefinitionValidatorRegistry` (required)

Module `issuerservice-presentation-attestations`
------------------------------------------------
**Artifact:** org.eclipse.edc:issuerservice-presentation-attestations:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.issuerservice.issuance.attestations.PresentationAttestationExtension`
**Name:** "VerifiablePresentation Attestations Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactoryRegistry` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionValidatorRegistry` (required)

Module `keypair-api`
--------------------
**Artifact:** org.eclipse.edc:keypair-api:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.keypair.KeyPairResourceManagementApiExtension`
**Name:** "KeyPairResource management Identity API Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.identityhub.spi.keypair.KeyPairService` (required)
- `org.eclipse.edc.identityhub.spi.authorization.AuthorizationService` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)

Module `local-did-publisher`
----------------------------
**Artifact:** org.eclipse.edc:local-did-publisher:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.publisher.did.local.LocalDidPublisherExtension`
**Name:** "Local DID publisher extension"

**Overview:** No overview provided.


### Configuration

| Key                 | Required | Type     | Default | Pattern | Min | Max | Description              |
| ------------------- | -------- | -------- | ------- | ------- | --- | --- | ------------------------ |
| `web.http.did.port` | `*`      | `string` | `10100` |         |     |     | Port for did api context |
| `web.http.did.path` | `*`      | `string` | `/`     |         |     |     | Path for did api context |

#### Provided services
- `org.eclipse.edc.identityhub.spi.did.events.DidDocumentObservable`

#### Referenced (injected) services
- `org.eclipse.edc.identityhub.spi.did.DidDocumentPublisherRegistry` (required)
- `org.eclipse.edc.identityhub.spi.did.store.DidResourceStore` (required)
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.web.spi.configuration.PortMappingRegistry` (required)
- `org.eclipse.edc.identityhub.spi.did.DidWebParser` (optional)
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.event.EventRouter` (required)

Module `local-statuslist-publisher`
-----------------------------------
**Artifact:** org.eclipse.edc:local-statuslist-publisher:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.issuerservice.publisher.LocalStatusListCredentialPublisherExtension`
**Name:** "IssuerService Default Services Extension"

**Overview:** No overview provided.


### Configuration

| Key                               | Required | Type     | Default       | Pattern | Min | Max | Description                                          |
| --------------------------------- | -------- | -------- | ------------- | ------- | --- | --- | ---------------------------------------------------- |
| `web.http.statuslist.port`        | `*`      | `string` | `9999`        |         |     |     | Port of the status list credential web endpoint      |
| `web.http.statuslist.path`        | `*`      | `string` | `/statuslist` |         |     |     | Port of the status list credential web endpoint      |
| `edc.statuslist.callback.address` |          | `string` | ``            |         |     |     | Configures endpoint for reaching the StatusList API. |

#### Provided services
- `org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListCredentialPublisher`

#### Referenced (injected) services
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore` (required)
- `org.eclipse.edc.web.spi.configuration.PortMappingRegistry` (required)
- `org.eclipse.edc.spi.system.Hostname` (required)
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)

Module `participant-context-api`
--------------------------------
**Artifact:** org.eclipse.edc:participant-context-api:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.verifiablecredential.ParticipantContextManagementApiExtension`
**Name:** "ParticipantContext management Identity API Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService` (required)
- `org.eclipse.edc.identityhub.spi.authorization.AuthorizationService` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)

Module `presentation-api`
-------------------------
**Artifact:** org.eclipse.edc:presentation-api:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.PresentationApiExtension`
**Name:** "Presentation API Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry` (required)
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenVerifier` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.resolution.CredentialQueryResolver` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.VerifiablePresentationService` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService` (required)

Module `storage-api`
--------------------
**Artifact:** org.eclipse.edc:storage-api:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.StorageApiExtension`
**Name:** "Storage API Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry` (required)
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.CredentialWriter` (required)
- `org.eclipse.edc.identityhub.protocols.dcp.spi.DcpIssuerTokenVerifier` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService` (required)

Module `sts-account-provisioner`
--------------------------------
**Artifact:** org.eclipse.edc:sts-account-provisioner:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.common.provisioner.StsAccountProvisionerExtension`
**Name:** "STS Account Provisioner Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.identityhub.spi.participantcontext.StsAccountProvisioner`

#### Referenced (injected) services
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `org.eclipse.edc.spi.security.Vault` (required)
- `org.eclipse.edc.iam.identitytrust.sts.spi.service.StsClientSecretGenerator` (optional)
- `org.eclipse.edc.iam.identitytrust.sts.spi.service.StsAccountService` (required)

Module `sts-account-service-local`
----------------------------------
**Artifact:** org.eclipse.edc:sts-account-service-local:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.sts.accountservice.StsAccountServiceExtension`
**Name:** "Local (embedded) STS Account Service Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.iam.identitytrust.sts.spi.service.StsAccountService`

#### Referenced (injected) services
- `org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.spi.security.Vault` (required)
- `org.eclipse.edc.iam.identitytrust.sts.spi.service.StsClientSecretGenerator` (optional)

Module `sts-api`
----------------
**Artifact:** org.eclipse.edc:sts-api:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.api.iam.identitytrust.sts.StsApiConfigurationExtension`
**Name:** "Secure Token Service API configuration"

**Overview:** No overview provided.


### Configuration

| Key                 | Required | Type     | Default    | Pattern | Min | Max | Description              |
| ------------------- | -------- | -------- | ---------- | ------- | --- | --- | ------------------------ |
| `web.http.sts.port` | `*`      | `string` | `9292`     |         |     |     | Port for sts api context |
| `web.http.sts.path` | `*`      | `string` | `/api/sts` |         |     |     | Path for sts api context |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.configuration.PortMappingRegistry` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.spi.system.apiversion.ApiVersionService` (required)

#### Class: `org.eclipse.edc.api.iam.identitytrust.sts.SecureTokenServiceApiExtension`
**Name:** "Secure Token Service API"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.iam.identitytrust.sts.spi.service.StsAccountService` (required)
- `org.eclipse.edc.iam.identitytrust.sts.spi.service.StsClientTokenGeneratorService` (required)
- `org.eclipse.edc.web.spi.WebService` (required)

Module `sts-client-store-sql`
-----------------------------
**Artifact:** org.eclipse.edc:sts-client-store-sql:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.iam.identitytrust.sts.store.SqlStsClientStoreExtension`
**Name:** "SQL sts accounts store"

**Overview:** No overview provided.


### Configuration

| Key                                  | Required | Type     | Default   | Pattern | Min | Max | Description               |
| ------------------------------------ | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.stsclient.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.iam.identitytrust.sts.store.schema.StsClientStatements` (optional)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)

Module `sts-core`
-----------------
**Artifact:** org.eclipse.edc:sts-core:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.iam.identitytrust.sts.defaults.StsDefaultServicesExtension`
**Name:** "Secure Token Service Default Services"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore`

#### Referenced (injected) services
- `org.eclipse.edc.spi.query.CriterionOperatorRegistry` (required)

#### Class: `org.eclipse.edc.iam.identitytrust.sts.EmbeddedStsServiceExtension`
**Name:** "Local (embedded) STS Account Service Extension"

**Overview:** No overview provided.


### Configuration

| Key                            | Required | Type     | Default | Pattern | Min | Max | Description                                                         |
| ------------------------------ | -------- | -------- | ------- | ------- | --- | --- | ------------------------------------------------------------------- |
| `edc.iam.sts.token.expiration` | `*`      | `string` | `5`     |         |     |     | Self-issued ID Token expiration in minutes. By default is 5 minutes |

#### Provided services
- `org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService`
- `org.eclipse.edc.iam.identitytrust.sts.spi.service.StsClientTokenGeneratorService`

#### Referenced (injected) services
- `java.time.Clock` (required)
- `org.eclipse.edc.jwt.signer.spi.JwsSignerProvider` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.iam.identitytrust.sts.spi.service.StsAccountService` (required)
- `org.eclipse.edc.identityhub.spi.keypair.KeyPairService` (required)

Module `test-attestations`
--------------------------
**Artifact:** org.eclipse.edc:test-attestations:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.test.e2e.tck.attestation.TckTestAttestationsExtension`
**Name:** "TCK Test Attestations Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactoryRegistry` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionValidatorRegistry` (required)

Module `verifiable-credentials-api`
-----------------------------------
**Artifact:** org.eclipse.edc:verifiable-credentials-api:0.15.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.verifiablecredentials.VerifiableCredentialApiExtension`
**Name:** "VerifiableCredentials API Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore` (required)
- `org.eclipse.edc.identityhub.spi.authorization.AuthorizationService` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestManager` (required)

