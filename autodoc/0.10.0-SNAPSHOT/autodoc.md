Module `api-configuration`
--------------------------
_artifact: _org.eclipse.edc:api-configuration:0.10.0-SNAPSHOT

_Categories: None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.configuration.IdentityApiConfigurationExtension`
**Name:** "Identity API Extension"

**Overview:** No overview provided.


### Configuration: _None_

##### Provided services:
- `org.eclipse.edc.identityhub.spi.AuthorizationService`

##### Referenced (injected) services:
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService` (required)
- `org.eclipse.edc.web.spi.configuration.WebServiceConfigurer` (required)
- `org.eclipse.edc.web.spi.WebServer` (required)
- `org.eclipse.edc.spi.security.Vault` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.spi.system.apiversion.ApiVersionService` (required)

Module `credential-watchdog`
----------------------------
_artifact: _org.eclipse.edc:credential-watchdog:0.10.0-SNAPSHOT

_Categories: None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.common.credentialwatchdog.CredentialWatchdogExtension`
**Name:** "VerifiableCredential Watchdog Extension"

**Overview:** No overview provided.


### Configuration: 

| Key                                      | Required | Type      | Default                | Pattern | Min | Max | Description                                                                                                                                      |
| ---------------------------------------- | -------- | --------- | ---------------------- | ------- | --- | --- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| `edc.iam.credential.status.check.period` |          | `integer` | `60`                   |         |     |     | Period (in seconds) at which the Watchdog thread checks all stored credentials for their status. Configuring a number <=0 disables the Watchdog. |
| `edc.iam.credential.status.check.delay`  |          | `integer` | `random number [1..5]` |         |     |     | Initial delay (in seconds) before the Watchdog thread begins its work.                                                                           |

##### Provided services:
_None_

##### Referenced (injected) services:
- `org.eclipse.edc.spi.system.ExecutorInstrumentation` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialStatusCheckService` (required)
- `org.eclipse.edc.identityhub.spi.store.CredentialStore` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)

Module `did-api`
----------------
_artifact: _org.eclipse.edc:did-api:0.10.0-SNAPSHOT

_Categories: None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.didmanagement.DidManagementApiExtension`
**Name:** "DID management Identity API Extension"

**Overview:** No overview provided.


### Configuration: _None_

##### Provided services:
_None_

##### Referenced (injected) services:
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.identithub.spi.did.DidDocumentService` (required)
- `org.eclipse.edc.identityhub.spi.AuthorizationService` (required)

Module `did-spi`
----------------
_name: _Identity Hub DID services
_artifact: _org.eclipse.edc:did-spi:0.10.0-SNAPSHOT

_Categories: None_

### Extension points
  - `org.eclipse.edc.identithub.spi.did.DidDocumentPublisher`
  - `org.eclipse.edc.identithub.spi.did.DidWebParser`
  - `org.eclipse.edc.identithub.spi.did.store.DidResourceStore`

### Extensions
Module `identity-hub-core`
--------------------------
_artifact: _org.eclipse.edc:identity-hub-core:0.10.0-SNAPSHOT

_Categories: None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.core.CoreServicesExtension`
**Name:** "IdentityHub Core Services Extension"

**Overview:**  This extension provides core services for the IdentityHub that are not intended to be user-replaceable.



### Configuration: _None_

##### Provided services:
- `org.eclipse.edc.identityhub.spi.verification.AccessTokenVerifier`
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.resolution.CredentialQueryResolver`
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.PresentationCreatorRegistry`
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.VerifiablePresentationService`
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialStatusCheckService`

##### Referenced (injected) services:
- `org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.identityhub.spi.store.CredentialStore` (required)
- `org.eclipse.edc.identityhub.spi.ScopeToCriterionTransformer` (required)
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
- `org.eclipse.edc.identityhub.spi.store.KeyPairResourceStore` (required)
- `org.eclipse.edc.keys.spi.LocalPublicKeyService` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService` (required)
- `org.eclipse.edc.jwt.signer.spi.JwsSignerProvider` (required)

#### Class: `org.eclipse.edc.identityhub.DefaultServicesExtension`
**Name:** "IdentityHub Default Services Extension"

**Overview:**  This extension provides core services for the IdentityHub that are not intended to be user-replaceable.



### Configuration: 

| Key                                            | Required | Type   | Default  | Pattern | Min | Max | Description                                                                  |
| ---------------------------------------------- | -------- | ------ | -------- | ------- | --- | --- | ---------------------------------------------------------------------------- |
| `edc.iam.credential.revocation.cache.validity` |          | `long` | `900000` |         |     |     | Validity period of cached StatusList2021 credential entries in milliseconds. |

##### Provided services:
- `org.eclipse.edc.identityhub.spi.store.CredentialStore`
- `org.eclipse.edc.identityhub.spi.store.ParticipantContextStore`
- `org.eclipse.edc.identityhub.spi.store.KeyPairResourceStore`
- `org.eclipse.edc.identityhub.spi.ScopeToCriterionTransformer`
- `org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry`
- `org.eclipse.edc.iam.identitytrust.spi.verification.SignatureSuiteRegistry`
- `org.eclipse.edc.jwt.signer.spi.JwsSignerProvider`

##### Referenced (injected) services:
- `org.eclipse.edc.token.spi.TokenValidationRulesRegistry` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.keys.spi.PrivateKeyResolver` (required)

Module `identity-hub-credentials-store-sql`
-------------------------------------------
_artifact: _org.eclipse.edc:identity-hub-credentials-store-sql:0.10.0-SNAPSHOT

_Categories: None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.store.sql.credentials.SqlCredentialStoreExtension`
**Name:** "CredentialResource SQL Store Extension"

**Overview:** No overview provided.


### Configuration: 

| Key                                    | Required | Type     | Default   | Pattern | Min | Max | Description                                  |
| -------------------------------------- | -------- | -------- | --------- | ------- | --- | --- | -------------------------------------------- |
| ~~edc.datasource.credentials.name~~    |          | `string` | `default` |         |     |     | Datasource name for the DidResource database |
| `edc.sql.store.credentials.datasource` |          | `string` | `default` |         |     |     | The datasource to be used                    |

##### Provided services:
- `org.eclipse.edc.identityhub.spi.store.CredentialStore`

##### Referenced (injected) services:
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.identityhub.store.sql.credentials.CredentialStoreStatements` (optional)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)

Module `identity-hub-did`
-------------------------
_artifact: _org.eclipse.edc:identity-hub-did:0.10.0-SNAPSHOT

_Categories: None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.did.defaults.DidDefaultServicesExtension`
**Name:** "DID Default Services Extension"

**Overview:** No overview provided.


### Configuration: _None_

##### Provided services:
- `org.eclipse.edc.identithub.spi.did.store.DidResourceStore`

##### Referenced (injected) services:
- `org.eclipse.edc.spi.query.CriterionOperatorRegistry` (required)

#### Class: `org.eclipse.edc.identityhub.did.DidServicesExtension`
**Name:** "DID Service Extension"

**Overview:** No overview provided.


### Configuration: _None_

##### Provided services:
- `org.eclipse.edc.identithub.spi.did.DidDocumentPublisherRegistry`
- `org.eclipse.edc.identithub.spi.did.DidDocumentService`

##### Referenced (injected) services:
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.identithub.spi.did.store.DidResourceStore` (required)
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `org.eclipse.edc.keys.spi.KeyParserRegistry` (required)
- `org.eclipse.edc.identityhub.spi.store.ParticipantContextStore` (required)

Module `identity-hub-did-store-sql`
-----------------------------------
_artifact: _org.eclipse.edc:identity-hub-did-store-sql:0.10.0-SNAPSHOT

_Categories: None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.did.store.sql.SqlDidResourceStoreExtension`
**Name:** "DID Resource SQL Store Extension"

**Overview:** No overview provided.


### Configuration: 

| Key                                    | Required | Type     | Default   | Pattern | Min | Max | Description                                  |
| -------------------------------------- | -------- | -------- | --------- | ------- | --- | --- | -------------------------------------------- |
| ~~edc.datasource.didresource.name~~    |          | `string` | `default` |         |     |     | Datasource name for the DidResource database |
| `edc.sql.store.didresource.datasource` |          | `string` | `default` |         |     |     | The datasource to be used                    |

##### Provided services:
- `org.eclipse.edc.identithub.spi.did.store.DidResourceStore`

##### Referenced (injected) services:
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.identityhub.did.store.sql.DidResourceStatements` (optional)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)

Module `identity-hub-keypair-store-sql`
---------------------------------------
_artifact: _org.eclipse.edc:identity-hub-keypair-store-sql:0.10.0-SNAPSHOT

_Categories: None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.store.sql.keypair.SqlKeyPairResourceStoreExtension`
**Name:** "KeyPair Resource SQL Store Extension"

**Overview:** No overview provided.


### Configuration: 

| Key                                | Required | Type     | Default   | Pattern | Min | Max | Description                                      |
| ---------------------------------- | -------- | -------- | --------- | ------- | --- | --- | ------------------------------------------------ |
| ~~edc.datasource.keypair.name~~    |          | `string` | `default` |         |     |     | Datasource name for the KeyPairResource database |
| `edc.sql.store.keypair.datasource` |          | `string` | `default` |         |     |     | The datasource to be used                        |

##### Provided services:
- `org.eclipse.edc.identityhub.spi.store.KeyPairResourceStore`

##### Referenced (injected) services:
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.identityhub.store.sql.keypair.KeyPairResourceStoreStatements` (optional)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)

Module `identity-hub-keypairs`
------------------------------
_artifact: _org.eclipse.edc:identity-hub-keypairs:0.10.0-SNAPSHOT

_Categories: None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.keypairs.KeyPairServiceExtension`
**Name:** "KeyPair Service Extension"

**Overview:** No overview provided.


### Configuration: _None_

##### Provided services:
- `org.eclipse.edc.identityhub.spi.keypair.KeyPairService`
- `org.eclipse.edc.identityhub.spi.keypair.events.KeyPairObservable`

##### Referenced (injected) services:
- `org.eclipse.edc.spi.security.Vault` (required)
- `org.eclipse.edc.identityhub.spi.store.KeyPairResourceStore` (required)
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.identityhub.spi.store.ParticipantContextStore` (required)

Module `identity-hub-participantcontext-store-sql`
--------------------------------------------------
_artifact: _org.eclipse.edc:identity-hub-participantcontext-store-sql:0.10.0-SNAPSHOT

_Categories: None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.store.sql.participantcontext.SqlParticipantContextStoreExtension`
**Name:** "ParticipantContext SQL Store Extension"

**Overview:** No overview provided.


### Configuration: 

| Key                                           | Required | Type     | Default   | Pattern | Min | Max | Description                                         |
| --------------------------------------------- | -------- | -------- | --------- | ------- | --- | --- | --------------------------------------------------- |
| ~~edc.datasource.participantcontext.name~~    |          | `string` | `default` |         |     |     | Datasource name for the ParticipantContext database |
| `edc.sql.store.participantcontext.datasource` |          | `string` | `default` |         |     |     | The datasource to be used                           |

##### Provided services:
- `org.eclipse.edc.identityhub.spi.store.ParticipantContextStore`

##### Referenced (injected) services:
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.identityhub.store.sql.participantcontext.ParticipantContextStoreStatements` (optional)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)

Module `identity-hub-participants`
----------------------------------
_artifact: _org.eclipse.edc:identity-hub-participants:0.10.0-SNAPSHOT

_Categories: None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.participantcontext.ParticipantContextCoordinatorExtension`
**Name:** "ParticipantContext Extension"

**Overview:** No overview provided.


### Configuration: _None_

##### Provided services:
_None_

##### Referenced (injected) services:
- `org.eclipse.edc.identithub.spi.did.DidDocumentService` (required)
- `org.eclipse.edc.identityhub.spi.keypair.KeyPairService` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService` (required)

#### Class: `org.eclipse.edc.identityhub.participantcontext.ParticipantContextExtension`
**Name:** "ParticipantContext Extension"

**Overview:** No overview provided.


### Configuration: _None_

##### Provided services:
- `org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService`
- `org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextObservable`

##### Referenced (injected) services:
- `org.eclipse.edc.identityhub.spi.store.ParticipantContextStore` (required)
- `org.eclipse.edc.spi.security.Vault` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.identityhub.spi.keypair.KeyPairService` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `org.eclipse.edc.identithub.spi.did.store.DidResourceStore` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.AccountProvisioner` (optional)

Module `identityhub-api-authentication`
---------------------------------------
_artifact: _org.eclipse.edc:identityhub-api-authentication:0.10.0-SNAPSHOT

_Categories: None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.ApiAuthenticationExtension`
**Name:** "Identity API Authentication Extension"

**Overview:** No overview provided.


### Configuration: _None_

##### Provided services:
_None_

##### Referenced (injected) services:
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService` (required)
- `org.eclipse.edc.spi.security.Vault` (required)

Module `identityhub-api-authorization`
--------------------------------------
_artifact: _org.eclipse.edc:identityhub-api-authorization:0.10.0-SNAPSHOT

_Categories: None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.ApiAuthorizationExtension`
**Name:** "Identity API Authorization Extension"

**Overview:** No overview provided.


### Configuration: _None_

##### Provided services:
- `org.eclipse.edc.identityhub.spi.AuthorizationService`

##### Referenced (injected) services:
_None_

Module `keypair-api`
--------------------
_artifact: _org.eclipse.edc:keypair-api:0.10.0-SNAPSHOT

_Categories: None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.keypair.KeyPairResourceManagementApiExtension`
**Name:** "KeyPairResource management Identity API Extension"

**Overview:** No overview provided.


### Configuration: _None_

##### Provided services:
_None_

##### Referenced (injected) services:
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.identityhub.spi.keypair.KeyPairService` (required)
- `org.eclipse.edc.identityhub.spi.AuthorizationService` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)

Module `local-did-publisher`
----------------------------
_artifact: _org.eclipse.edc:local-did-publisher:0.10.0-SNAPSHOT

_Categories: None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.publisher.did.local.LocalDidPublisherExtension`
**Name:** "Local DID publisher extension"

**Overview:** No overview provided.


### Configuration: _None_

##### Provided services:
- `org.eclipse.edc.identithub.spi.did.events.DidDocumentObservable`

##### Referenced (injected) services:
- `org.eclipse.edc.identithub.spi.did.DidDocumentPublisherRegistry` (required)
- `org.eclipse.edc.identithub.spi.did.store.DidResourceStore` (required)
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.web.spi.configuration.WebServiceConfigurer` (required)
- `org.eclipse.edc.web.spi.WebServer` (required)
- `org.eclipse.edc.identithub.spi.did.DidWebParser` (optional)
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.event.EventRouter` (required)

Module `participant-context-api`
--------------------------------
_artifact: _org.eclipse.edc:participant-context-api:0.10.0-SNAPSHOT

_Categories: None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.participantcontext.ParticipantContextManagementApiExtension`
**Name:** "ParticipantContext management Identity API Extension"

**Overview:** No overview provided.


### Configuration: _None_

##### Provided services:
_None_

##### Referenced (injected) services:
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService` (required)
- `org.eclipse.edc.identityhub.spi.AuthorizationService` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)

Module `presentation-api`
-------------------------
_artifact: _org.eclipse.edc:presentation-api:0.10.0-SNAPSHOT

_Categories: None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.PresentationApiExtension`
**Name:** "Presentation API Extension"

**Overview:** No overview provided.


### Configuration: _None_

##### Provided services:
_None_

##### Referenced (injected) services:
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry` (required)
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.identityhub.spi.verification.AccessTokenVerifier` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.resolution.CredentialQueryResolver` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.VerifiablePresentationService` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService` (required)
- `org.eclipse.edc.spi.system.apiversion.ApiVersionService` (required)

Module `sts-account-provisioner`
--------------------------------
_artifact: _org.eclipse.edc:sts-account-provisioner:0.10.0-SNAPSHOT

_Categories: None_

### Extension points
  - `org.eclipse.edc.identityhub.common.provisioner.StsClientSecretGenerator`

### Extensions
#### Class: `org.eclipse.edc.identityhub.common.provisioner.StsAccountProvisionerExtension`
**Name:** "STS Account Provisioner Extension"

**Overview:** No overview provided.


### Configuration: _None_

##### Provided services:
- `org.eclipse.edc.identityhub.spi.participantcontext.AccountProvisioner`

##### Referenced (injected) services:
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `org.eclipse.edc.identityhub.spi.keypair.KeyPairService` (required)
- `org.eclipse.edc.identithub.spi.did.DidDocumentService` (required)
- `org.eclipse.edc.iam.identitytrust.sts.spi.store.StsClientStore` (optional)
- `org.eclipse.edc.spi.security.Vault` (required)
- `org.eclipse.edc.identityhub.common.provisioner.StsClientSecretGenerator` (optional)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)

Module `verifiable-credentials-api`
-----------------------------------
_artifact: _org.eclipse.edc:verifiable-credentials-api:0.10.0-SNAPSHOT

_Categories: None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.verifiablecredentials.VerifiableCredentialApiExtension`
**Name:** "VerifiableCredentials API Extension"

**Overview:** No overview provided.


### Configuration: _None_

##### Provided services:
_None_

##### Referenced (injected) services:
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.identityhub.spi.store.CredentialStore` (required)
- `org.eclipse.edc.identityhub.spi.AuthorizationService` (required)

