Module `api-configuration`
--------------------------
**Artifact:** org.eclipse.edc:api-configuration:0.12.0-SNAPSHOT

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

Module `credential-watchdog`
----------------------------
**Artifact:** org.eclipse.edc:credential-watchdog:0.12.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.common.credentialwatchdog.CredentialWatchdogExtension`
**Name:** "VerifiableCredential Watchdog Extension"

**Overview:** No overview provided.


### Configuration

| Key                                      | Required | Type     | Default | Pattern | Min | Max | Description                                                                                                                                      |
| ---------------------------------------- | -------- | -------- | ------- | ------- | --- | --- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| `edc.iam.credential.status.check.period` | `*`      | `string` | `60`    |         |     |     | Period (in seconds) at which the Watchdog thread checks all stored credentials for their status. Configuring a number <=0 disables the Watchdog. |
| `edc.iam.credential.status.check.delay`  |          | `string` | ``      |         |     |     | Initial delay (in seconds) before the Watchdog thread begins its work.                                                                           |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.spi.system.ExecutorInstrumentation` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialStatusCheckService` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)

Module `did-api`
----------------
**Artifact:** org.eclipse.edc:did-api:0.12.0-SNAPSHOT

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
**Artifact:** org.eclipse.edc:did-spi:0.12.0-SNAPSHOT

**Categories:** _None_

### Extension points
  - `org.eclipse.edc.identityhub.spi.did.DidWebParser`
  - `org.eclipse.edc.identityhub.spi.did.store.DidResourceStore`
  - `org.eclipse.edc.identityhub.spi.did.DidDocumentPublisher`

### Extensions
Module `identity-hub-core`
--------------------------
**Artifact:** org.eclipse.edc:identity-hub-core:0.12.0-SNAPSHOT

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
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialStatusCheckService`

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

#### Class: `org.eclipse.edc.identityhub.DefaultServicesExtension`
**Name:** "IdentityHub Default Services Extension"

**Overview:**  This extension provides core services for the IdentityHub that are not intended to be user-replaceable.



### Configuration

| Key                                            | Required | Type     | Default  | Pattern | Min | Max | Description                                                                                  |
| ---------------------------------------------- | -------- | -------- | -------- | ------- | --- | --- | -------------------------------------------------------------------------------------------- |
| `edc.iam.accesstoken.jti.validation`           | `*`      | `string` | `false`  |         |     |     | Activates the JTI check: access tokens can only be used once to guard against replay attacks |
| `edc.iam.credential.revocation.cache.validity` | `*`      | `string` | `900000` |         |     |     | Validity period of cached StatusList2021 credential entries in milliseconds.                 |

#### Provided services
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore`
- `org.eclipse.edc.identityhub.spi.participantcontext.store.ParticipantContextStore`
- `org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore`
- `org.eclipse.edc.identityhub.spi.transformation.ScopeToCriterionTransformer`
- `org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry`
- `org.eclipse.edc.iam.identitytrust.spi.verification.SignatureSuiteRegistry`
- `org.eclipse.edc.jwt.signer.spi.JwsSignerProvider`

#### Referenced (injected) services
- `org.eclipse.edc.token.spi.TokenValidationRulesRegistry` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.keys.spi.PrivateKeyResolver` (required)
- `org.eclipse.edc.jwt.validation.jti.JtiValidationStore` (required)

Module `identity-hub-credentials-store-sql`
-------------------------------------------
**Artifact:** org.eclipse.edc:identity-hub-credentials-store-sql:0.12.0-SNAPSHOT

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
**Artifact:** org.eclipse.edc:identity-hub-did:0.12.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.did.defaults.DidDefaultServicesExtension`
**Name:** "DID Default Services Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.identityhub.spi.did.store.DidResourceStore`

#### Referenced (injected) services
- `org.eclipse.edc.spi.query.CriterionOperatorRegistry` (required)

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

Module `identity-hub-did-store-sql`
-----------------------------------
**Artifact:** org.eclipse.edc:identity-hub-did-store-sql:0.12.0-SNAPSHOT

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
**Artifact:** org.eclipse.edc:identity-hub-keypair-store-sql:0.12.0-SNAPSHOT

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
**Artifact:** org.eclipse.edc:identity-hub-keypairs:0.12.0-SNAPSHOT

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
**Artifact:** org.eclipse.edc:identity-hub-participantcontext-store-sql:0.12.0-SNAPSHOT

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
**Artifact:** org.eclipse.edc:identity-hub-participants:0.12.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
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

Module `identityhub-api-authentication`
---------------------------------------
**Artifact:** org.eclipse.edc:identityhub-api-authentication:0.12.0-SNAPSHOT

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
**Artifact:** org.eclipse.edc:identityhub-api-authorization:0.12.0-SNAPSHOT

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

Module `issuer-api`
-------------------
**Artifact:** org.eclipse.edc:issuer-api:0.12.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.protocols.dcp.issuer.IssuerApiExtension`
**Name:** "Issuer API extension"

**Overview:** No overview provided.


### Configuration

| Key                        | Required | Type     | Default       | Pattern | Min | Max | Description                     |
| -------------------------- | -------- | -------- | ------------- | ------- | --- | --- | ------------------------------- |
| `web.http.issuer-api.port` | `*`      | `string` | `13132`       |         |     |     | Port for issuer-api api context |
| `web.http.issuer-api.path` | `*`      | `string` | `/api/issuer` |         |     |     | Path for issuer-api api context |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.spi.system.apiversion.ApiVersionService` (required)
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.web.spi.configuration.PortMappingRegistry` (required)

Module `keypair-api`
--------------------
**Artifact:** org.eclipse.edc:keypair-api:0.12.0-SNAPSHOT

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
**Artifact:** org.eclipse.edc:local-did-publisher:0.12.0-SNAPSHOT

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

Module `participant-context-api`
--------------------------------
**Artifact:** org.eclipse.edc:participant-context-api:0.12.0-SNAPSHOT

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
**Artifact:** org.eclipse.edc:presentation-api:0.12.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.PresentationApiExtension`
**Name:** "Presentation API Extension"

**Overview:** No overview provided.


### Configuration

| Key                          | Required | Type     | Default             | Pattern | Min | Max | Description                       |
| ---------------------------- | -------- | -------- | ------------------- | ------- | --- | --- | --------------------------------- |
| `web.http.presentation.port` | `*`      | `string` | `13131`             |         |     |     | Port for presentation api context |
| `web.http.presentation.path` | `*`      | `string` | `/api/presentation` |         |     |     | Path for presentation api context |

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
- `org.eclipse.edc.spi.system.apiversion.ApiVersionService` (required)
- `org.eclipse.edc.web.spi.configuration.PortMappingRegistry` (required)

Module `sts-account-provisioner`
--------------------------------
**Artifact:** org.eclipse.edc:sts-account-provisioner:0.12.0-SNAPSHOT

**Categories:** _None_

### Extension points
  - `org.eclipse.edc.identityhub.common.provisioner.StsClientSecretGenerator`

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
- `org.eclipse.edc.identityhub.common.provisioner.StsClientSecretGenerator` (optional)
- `org.eclipse.edc.identityhub.spi.participantcontext.StsAccountService` (optional)

Module `sts-account-service-local`
----------------------------------
**Artifact:** org.eclipse.edc:sts-account-service-local:0.12.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.sts.accountservice.LocalStsAccountServiceExtension`
**Name:** "Local (embedded) STS Account Service Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.identityhub.spi.participantcontext.StsAccountService`

#### Referenced (injected) services
- `org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)

Module `sts-account-service-remote`
-----------------------------------
**Artifact:** org.eclipse.edc:sts-account-service-remote:0.12.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.sts.accountservice.RemoteStsAccountServiceExtension`
**Name:** "Remote STS Account Service Extension"

**Overview:** No overview provided.


### Configuration

| Key                                      | Required | Type     | Default     | Pattern | Min | Max | Description                                                                                    |
| ---------------------------------------- | -------- | -------- | ----------- | ------- | --- | --- | ---------------------------------------------------------------------------------------------- |
| `edc.sts.account.api.url`                | `*`      | `string` | ``          |         |     |     | The base URL of the remote STS Accounts API                                                    |
| `edc.sts.accounts.api.auth.header.name`  | `*`      | `string` | `x-api-key` |         |     |     | The name of the Auth header to use. Could be 'Authorization', some custom auth header, etc.    |
| `edc.sts.accounts.api.auth.header.value` | `*`      | `string` | ``          |         |     |     | The value of the Auth header to use. Currently we only support static values, e.g. tokens etc. |

#### Provided services
- `org.eclipse.edc.identityhub.spi.participantcontext.StsAccountService`

#### Referenced (injected) services
- `org.eclipse.edc.http.spi.EdcHttpClient` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)

Module `verifiable-credentials-api`
-----------------------------------
**Artifact:** org.eclipse.edc:verifiable-credentials-api:0.12.0-SNAPSHOT

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

