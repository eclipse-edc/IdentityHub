Module `attestation-api`
------------------------
**Artifact:** org.eclipse.edc:attestation-api:1.0.0-SNAPSHOT

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
- `org.eclipse.edc.api.auth.spi.AuthorizationService` (required)

Module `common-core`
--------------------
**Artifact:** org.eclipse.edc:common-core:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.ScopeMappingExtension`
**Name:** "Scope Mapping Extension"

**Overview:** No overview provided.


### Configuration

| Key                                          | Required | Type     | Default | Pattern | Min | Max | Description                                                                                |
| -------------------------------------------- | -------- | -------- | ------- | ------- | --- | --- | ------------------------------------------------------------------------------------------ |
| `edc.identityhub.scope.<alias>.pattern`      | `*`      | `string` | ``      |         |     |     | The regular expression the scope string is matched against.                                |
| `edc.identityhub.scope.<alias>.leftoperand`  | `*`      | `string` | ``      |         |     |     | The left operand of the resulting criterion, may reference regex capture groups (e.g. $1)  |
| `edc.identityhub.scope.<alias>.operator`     | `*`      | `string` | ``      |         |     |     | The operator of the resulting criterion, e.g. 'contains'                                   |
| `edc.identityhub.scope.<alias>.rightoperand` | `*`      | `string` | ``      |         |     |     | The right operand of the resulting criterion, may reference regex capture groups (e.g. $1) |

#### Provided services
- `org.eclipse.edc.identityhub.spi.transformation.ScopeMappingRegistry`

#### Referenced (injected) services
_None_

#### Class: `org.eclipse.edc.identityhub.DefaultServicesExtension`
**Name:** "IdentityHub Default Services Extension"

**Overview:** No overview provided.


### Configuration

| Key                                  | Required | Type     | Default | Pattern | Min | Max | Description                                                                                      |
| ------------------------------------ | -------- | -------- | ------- | ------- | --- | --- | ------------------------------------------------------------------------------------------------ |
| `edc.iam.accesstoken.jti.validation` | `*`      | `string` | `false` |         |     |     | Activates the JTI check: access tokens can only be used once to guard against replay attacks     |
| `edc.sql.store.jti.cleanup.period`   | `*`      | `string` | `60`    |         |     |     | The period in seconds, in which expired JTI entries are deleted while the JTI check is activated |

#### Provided services
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore`
- `org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore`
- `org.eclipse.edc.identityhub.spi.transformation.ScopeToCriterionTransformer`
- `org.eclipse.edc.iam.decentralizedclaims.spi.verification.SignatureSuiteRegistry`
- `org.eclipse.edc.jwt.spi.signer.JwsSignerProvider`
- `org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore`
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore`

#### Referenced (injected) services
- `org.eclipse.edc.token.spi.TokenValidationRulesRegistry` (required)
- `org.eclipse.edc.keys.spi.PrivateKeyResolver` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.query.CriterionOperatorRegistry` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.jwt.validation.jti.JtiValidationStore` (required)
- `org.eclipse.edc.identityhub.spi.transformation.DiscriminatorMappingRegistry` (required)
- `org.eclipse.edc.identityhub.spi.transformation.ScopeMappingRegistry` (required)
- `org.eclipse.edc.spi.system.ExecutorInstrumentation` (required)

#### Class: `org.eclipse.edc.identityhub.DiscriminatorMappingExtension`
**Name:** "Discriminator Mapping Extension"

**Overview:** No overview provided.


### Configuration

| Key                                           | Required | Type     | Default | Pattern | Min | Max | Description                          |
| --------------------------------------------- | -------- | -------- | ------- | ------- | --- | --- | ------------------------------------ |
| `edc.identityhub.discriminator.<alias>.value` | `*`      | `string` | ``      |         |     |     | the full value for the discriminator |
| `edc.identityhub.discriminator.<alias>.alias` | `*`      | `string` | ``      |         |     |     | the discriminator alias              |

#### Provided services
- `org.eclipse.edc.identityhub.spi.transformation.DiscriminatorMappingRegistry`

#### Referenced (injected) services
- `org.eclipse.edc.spi.event.EventRouter` (required)

Module `credential-definition-api`
----------------------------------
**Artifact:** org.eclipse.edc:credential-definition-api:1.0.0-SNAPSHOT

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
- `org.eclipse.edc.api.auth.spi.AuthorizationService` (required)
- `org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerProfile` (required)

Module `credential-offer-api`
-----------------------------
**Artifact:** org.eclipse.edc:credential-offer-api:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.CredentialOfferApiExtension`
**Name:** "Storage API Extension"

**Overview:** No overview provided.


### Configuration

| Key                                     | Required | Type     | Default | Pattern | Min | Max | Description                                                                                                                                                                                                                                                                                                                                                        |
| --------------------------------------- | -------- | -------- | ------- | ------- | --- | --- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `edc.iam.issuer.endpoint.restrict`      | `*`      | `string` | `false` |         |     |     | Whether requests to an Issuer's IssuerService endpoint, which is taken from the Issuer's DID document, must use HTTPS and may only be sent to hosts that resolve to public addresses, and whether redirects are not followed. Should be enabled if participants or Issuers are not trusted to only publish endpoints of legitimate Issuers in their DID documents. |
| `edc.iam.issuer.endpoint.allowed.hosts` |          | `string` | ``      |         |     |     | Comma-separated list of hosts (host names or IP addresses) that requests to an Issuer's IssuerService endpoint may be sent to, although they use plain HTTP or resolve to loopback, link-local or private addresses, e.g. an Issuer running in the same cluster. Only has an effect if 'edc.iam.issuer.endpoint.restrict' is enabled.                              |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry` (required)
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.identityhub.protocols.dcp.spi.DcpIssuerTokenVerifier` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.offer.CredentialOfferService` (required)
- `org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry` (required)
- `org.eclipse.edc.http.spi.EdcHttpClient` (required)
- `okhttp3.OkHttpClient` (required)
- `dev.failsafe.RetryPolicy<okhttp3.Response>` (required)

Module `credential-offer-handler`
---------------------------------
**Artifact:** org.eclipse.edc:credential-offer-handler:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.credential.offer.handler.CredentialOfferHandlerExtension`
**Name:** "CredentialOfferHandlerExtension"

**Overview:** No overview provided.


### Configuration

| Key                                        | Required | Type     | Default | Pattern | Min | Max | Description                                                                                                                                                                                                                                                                                     |
| ------------------------------------------ | -------- | -------- | ------- | ------- | --- | --- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `edc.iam.credential.offer.trusted.issuers` |          | `string` | ``      |         |     |     | Comma-separated list of issuer DIDs whose credential offers automatically trigger a credential request. Offers of issuers the participant already holds credentials from do so as well. Offers of other issuers are only stored. If not set, offers of any issuer trigger a credential request. |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `org.eclipse.edc.identityhub.protocols.dcp.spi.DcpProfileRegistry` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestManager` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore` (required)
- `org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore` (required)

Module `credential-watchdog`
----------------------------
**Artifact:** org.eclipse.edc:credential-watchdog:1.0.0-SNAPSHOT

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
- `java.time.Clock` (required)

Module `credentials-api`
------------------------
**Artifact:** org.eclipse.edc:credentials-api:1.0.0-SNAPSHOT

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
- `org.eclipse.edc.api.auth.spi.AuthorizationService` (required)
- `org.eclipse.edc.issuerservice.spi.credentials.IssuerCredentialOfferService` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)

Module `credentials-api-configuration`
--------------------------------------
**Artifact:** org.eclipse.edc:credentials-api-configuration:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.CredentialsApiConfigurationExtension`
**Name:** "Storage API Extension"

**Overview:** No overview provided.


### Configuration

| Key                                | Required | Type     | Default            | Pattern | Min | Max | Description                                                                                                             |
| ---------------------------------- | -------- | -------- | ------------------ | ------- | --- | --- | ----------------------------------------------------------------------------------------------------------------------- |
| `web.http.credentials.port`        | `*`      | `string` | `13131`            |         |     |     | Port for credentials api context                                                                                        |
| `web.http.credentials.path`        | `*`      | `string` | `/api/credentials` |         |     |     | Path for credentials api context                                                                                        |
| `web.http.credentials.maxbodysize` | `*`      | `string` | `5242880`          |         |     |     | Maximum size in bytes of request bodies accepted by the credentials api context. Larger requests are rejected with 413. |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.spi.system.apiversion.ApiVersionService` (required)
- `org.eclipse.edc.web.spi.configuration.PortMappingRegistry` (required)
- `org.eclipse.edc.web.spi.WebService` (required)

Module `dcp-core`
-----------------
**Artifact:** org.eclipse.edc:dcp-core:1.0.0-SNAPSHOT

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
**Artifact:** org.eclipse.edc:dcp-identityhub-core:1.0.0-SNAPSHOT

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
**Artifact:** org.eclipse.edc:dcp-issuer-api:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.protocols.dcp.issuer.IssuerApiExtension`
**Name:** "Issuer API extension"

**Overview:** No overview provided.


### Configuration

| Key                             | Required | Type     | Default         | Pattern | Min | Max | Description                                                                                                          |
| ------------------------------- | -------- | -------- | --------------- | ------- | --- | --- | -------------------------------------------------------------------------------------------------------------------- |
| `web.http.issuance.port`        | `*`      | `string` | `13132`         |         |     |     | Port for issuance api context                                                                                        |
| `web.http.issuance.path`        | `*`      | `string` | `/api/issuance` |         |     |     | Path for issuance api context                                                                                        |
| `web.http.issuance.maxbodysize` | `*`      | `string` | `5242880`       |         |     |     | Maximum size in bytes of request bodies accepted by the issuance api context. Larger requests are rejected with 413. |

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
- `org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.process.IssuanceProcessService` (required)
- `org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerMetadataService` (required)

Module `dcp-issuer-core`
------------------------
**Artifact:** org.eclipse.edc:dcp-issuer-core:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.protocols.dcp.issuer.DcpIssuerCoreExtension`
**Name:** "DCP Issuer Core Extension"

**Overview:** No overview provided.


### Configuration

| Key                                                       | Required | Type     | Default         | Pattern | Min | Max | Description                                                                                                                                                                                                                                        |
| --------------------------------------------------------- | -------- | -------- | --------------- | ------- | --- | --- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `edc.issuer.issuance.state-machine.iteration-wait-millis` | `*`      | `string` | `1000`          |         |     |     | The iteration wait time in milliseconds in the state machine.                                                                                                                                                                                      |
| `edc.issuer.issuance.state-machine.batch-size`            | `*`      | `string` | `10`            |         |     |     | The number of entities to be processed on every iteration. They are leased together, so the lease must outlast the processing of all of them.                                                                                                      |
| `edc.issuer.issuance.state-machine.lease-duration-millis` | `*`      | `string` | `300000`        |         |     |     | How long the entities of a batch are leased, in milliseconds. It must be longer than the processing of a whole batch, i.e. longer than the batch size times the call timeout, otherwise another runtime may process the same entities.             |
| `edc.issuer.issuance.send.retry.limit`                    | `*`      | `string` | `80`            |         |     |     | How many times a failed operation is retried before it fails with an error. With the default delays, the retries take about six hours.                                                                                                             |
| `edc.issuer.issuance.send.retry.base-delay.ms`            | `*`      | `string` | `1000`          |         |     |     | The base delay of retries in milliseconds. The n-th retry waits the base delay times 2 to the power of n, but at most the maximum delay.                                                                                                           |
| `edc.issuer.issuance.send.retry.max-delay.ms`             | `*`      | `string` | `300000`        |         |     |     | The maximum delay between two retries in milliseconds.                                                                                                                                                                                             |
| `edc.issuer.issuance.send.connect-timeout.ms`             | `*`      | `string` | `5000`          |         |     |     | How long it may take to connect to the counterparty when a message is sent, in milliseconds.                                                                                                                                                       |
| `edc.issuer.issuance.send.call-timeout.ms`                | `*`      | `string` | `15000`         |         |     |     | How long it may take to send a message and receive the response, in milliseconds. A message that times out is not retried right away, but by the state machine, like any other failure.                                                            |
| `edc.issuer.dcp.profile`                                  | `*`      | `string` | `vc20-bssl/jwt` |         |     |     | The DCP profile of the issuer, e.g. 'vc20-bssl/jwt' (VCDM 2.0 Bitstring Status List) or 'vc11-sl2021/jwt' (VCDM 1.1 StatusList2021). It determines the kind of status list that holds the status of the issued credentials, whatever their format. |
| `edc.iam.accesstoken.jti.validation`                      | `*`      | `string` | `false`         |         |     |     | Activates the JTI check: access tokens can only be used once to guard against replay attacks                                                                                                                                                       |
| `edc.issuance.anonymous.allowed`                          | `*`      | `string` | `false`         |         |     |     | Allow anonymous onboarding                                                                                                                                                                                                                         |

#### Provided services
- `org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerProfile`
- `org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerService`
- `org.eclipse.edc.identityhub.protocols.dcp.spi.DcpHolderTokenVerifier`
- `org.eclipse.edc.issuerservice.spi.issuance.delivery.CredentialStorageClient`
- `org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerMetadataService`
- `org.eclipse.edc.iam.decentralizedclaims.spi.CredentialServiceUrlResolver`

#### Referenced (injected) services
- `org.eclipse.edc.token.spi.TokenValidationRulesRegistry` (required)
- `org.eclipse.edc.token.spi.TokenValidationService` (required)
- `org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore` (required)
- `org.eclipse.edc.issuerservice.spi.holder.store.HolderStore` (required)
- `org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationPipeline` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleDefinitionEvaluator` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `java.time.Clock` (required)
- `okhttp3.OkHttpClient` (required)
- `org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService` (required)
- `org.eclipse.edc.spi.security.Vault` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry` (required)
- `org.eclipse.edc.identityhub.protocols.dcp.spi.DcpProfileRegistry` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry` (required)
- `org.eclipse.edc.jwt.validation.jti.JtiValidationStore` (optional)
- `org.eclipse.edc.spi.telemetry.Telemetry` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.events.IssuanceObservable` (required)

Module `did-api`
----------------
**Artifact:** org.eclipse.edc:did-api:1.0.0-SNAPSHOT

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
- `org.eclipse.edc.api.auth.spi.AuthorizationService` (required)

Module `did-spi`
----------------
**Name:** Identity Hub DID services
**Artifact:** org.eclipse.edc:did-spi:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
  - `org.eclipse.edc.identityhub.spi.did.DidDocumentPublisher`
  - `org.eclipse.edc.identityhub.spi.did.store.DidResourceStore`
  - `org.eclipse.edc.identityhub.spi.did.DidWebParser`

### Extensions
Module `holder-api`
-------------------
**Artifact:** org.eclipse.edc:holder-api:1.0.0-SNAPSHOT

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
- `org.eclipse.edc.api.auth.spi.AuthorizationService` (required)

Module `holder-credential-offer-store-sql`
------------------------------------------
**Artifact:** org.eclipse.edc:holder-credential-offer-store-sql:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.store.sql.credentialoffer.schema.SqlCredentialOfferStoreExtension`
**Name:** "Holder Credential Offer SQL Store Extension"

**Overview:** No overview provided.


### Configuration

| Key                                        | Required | Type     | Default   | Pattern | Min | Max | Description               |
| ------------------------------------------ | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.credentialoffer.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.identityhub.store.sql.credentialoffer.schema.CredentialOfferStoreStatements` (optional)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)
- `java.time.Clock` (required)

Module `holder-credential-request-store-sql`
--------------------------------------------
**Artifact:** org.eclipse.edc:holder-credential-request-store-sql:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.store.sql.credentialrequest.schema.SqlHolderCredentialRequestStoreExtension`
**Name:** "Holder Credential Request SQL Store Extension"

**Overview:** No overview provided.


### Configuration

| Key                                                              | Required | Type     | Default   | Pattern | Min | Max | Description                                                                                                                                                                                                                            |
| ---------------------------------------------------------------- | -------- | -------- | --------- | ------- | --- | --- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `edc.iam.credential.request.state-machine.iteration-wait-millis` | `*`      | `string` | `1000`    |         |     |     | The iteration wait time in milliseconds in the state machine.                                                                                                                                                                          |
| `edc.iam.credential.request.state-machine.batch-size`            | `*`      | `string` | `10`      |         |     |     | The number of entities to be processed on every iteration. They are leased together, so the lease must outlast the processing of all of them.                                                                                          |
| `edc.iam.credential.request.state-machine.lease-duration-millis` | `*`      | `string` | `300000`  |         |     |     | How long the entities of a batch are leased, in milliseconds. It must be longer than the processing of a whole batch, i.e. longer than the batch size times the call timeout, otherwise another runtime may process the same entities. |
| `edc.iam.credential.request.send.retry.limit`                    | `*`      | `string` | `80`      |         |     |     | How many times a failed operation is retried before it fails with an error. With the default delays, the retries take about six hours.                                                                                                 |
| `edc.iam.credential.request.send.retry.base-delay.ms`            | `*`      | `string` | `1000`    |         |     |     | The base delay of retries in milliseconds. The n-th retry waits the base delay times 2 to the power of n, but at most the maximum delay.                                                                                               |
| `edc.iam.credential.request.send.retry.max-delay.ms`             | `*`      | `string` | `300000`  |         |     |     | The maximum delay between two retries in milliseconds.                                                                                                                                                                                 |
| `edc.iam.credential.request.send.connect-timeout.ms`             | `*`      | `string` | `5000`    |         |     |     | How long it may take to connect to the counterparty when a message is sent, in milliseconds.                                                                                                                                           |
| `edc.iam.credential.request.send.call-timeout.ms`                | `*`      | `string` | `15000`   |         |     |     | How long it may take to send a message and receive the response, in milliseconds. A message that times out is not retried right away, but by the state machine, like any other failure.                                                |
| `edc.sql.store.credentialrequest.datasource`                     | `*`      | `string` | `default` |         |     |     | The datasource to be used                                                                                                                                                                                                              |

#### Provided services
- `org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.identityhub.store.sql.credentialrequest.schema.HolderCredentialRequestStoreStatements` (optional)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)
- `java.time.Clock` (required)

Module `identity-api-authentication-oauth2`
-------------------------------------------
**Artifact:** org.eclipse.edc:identity-api-authentication-oauth2:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.Oauth2JwtAuthenticationExtension`
**Name:** "Identity API OAuth2/JWT Authentication Extension"

**Overview:** No overview provided.


### Configuration

| Key                                  | Required | Type     | Default  | Pattern | Min | Max | Description                                                                                                                              |
| ------------------------------------ | -------- | -------- | -------- | ------- | --- | --- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `edc.iam.oauth2.issuer`              |          | `string` | ``       |         |     |     | Expected value for the 'iss' claim for tokens coming from the OAuth2 server. If this is omitted, the 'iss' claim is not validated        |
| `edc.iam.oauth2.audience`            |          | `string` | ``       |         |     |     | Audience that the 'aud' claim of tokens coming from the OAuth2 server must contain. If this is omitted, the 'aud' claim is not validated |
| `edc.iam.oauth2.jwks.url`            | `*`      | `string` | ``       |         |     |     | Absolute URL where the JWKS of the OAuth2 server is hosted                                                                               |
| `edc.iam.oauth2.jwks.cache.validity` | `*`      | `string` | `300000` |         |     |     | Time (in ms) that cached JWKS are cached                                                                                                 |
| `edc.iam.oauth2.validity.leeway`     | `*`      | `string` | `5`      |         |     |     | Leeway (in sec) to allow when validating token time-based claims                                                                         |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.participantcontext.spi.service.ParticipantContextService` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.token.spi.TokenValidationService` (required)
- `org.eclipse.edc.keys.spi.KeyParserRegistry` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)

Module `identity-api-authorization-oauth2`
------------------------------------------
**Artifact:** org.eclipse.edc:identity-api-authorization-oauth2:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.Oauth2AuthorizationExtension`
**Name:** "Identity API OAuth2 Authorization Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.api.auth.spi.AuthorizationService`

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)

Module `identity-api-configuration`
-----------------------------------
**Artifact:** org.eclipse.edc:identity-api-configuration:1.0.0-SNAPSHOT

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
- `org.eclipse.edc.api.auth.spi.AuthorizationService`

#### Referenced (injected) services
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.web.spi.configuration.PortMappingRegistry` (required)
- `org.eclipse.edc.spi.system.apiversion.ApiVersionService` (required)
- `org.eclipse.edc.web.spi.WebService` (required)

Module `identity-hub-core`
--------------------------
**Artifact:** org.eclipse.edc:identity-hub-core:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.core.CoreServicesExtension`
**Name:** "IdentityHub Core Services Extension"

**Overview:**  This extension provides core services for the IdentityHub that are not intended to be user-replaceable.



### Configuration

| Key                                                              | Required | Type     | Default  | Pattern | Min | Max | Description                                                                                                                                                                                                                                                                                                                                                        |
| ---------------------------------------------------------------- | -------- | -------- | -------- | ------- | --- | --- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `edc.iam.credential.request.status.poll.interval`                | `*`      | `string` | `30000`  |         |     |     | Interval in milliseconds at which the Issuer is asked about the status of credential requests that are still awaiting their credentials. Each run is delayed by up to a fifth of it at random, so that runtimes do not poll in lockstep. Configuring a number <= 0 disables polling, and with it the timeouts of requests awaiting their credentials.              |
| `edc.iam.credential.request.status.poll.min.age`                 | `*`      | `string` | `30000`  |         |     |     | Time in milliseconds a credential request waits for its credentials before the Issuer is first asked about its status, as most Issuers deliver them within that time                                                                                                                                                                                               |
| `edc.iam.credential.request.status.poll.max.interval`            | `*`      | `string` | `900000` |         |     |     | Maximum time in milliseconds between two status queries for the same credential request. A request is asked about less often the longer it waits for its credentials, up to this interval.                                                                                                                                                                         |
| ~~edc.iam.credential.request.bearer.access.scope~~               |          | `string` | ``       |         |     |     | The scope that will be used to issue an access token to be included in the Self-Issued ID token in the 'token' claim. By default, no token will be generated.                                                                                                                                                                                                      |
| `edc.iam.credential.request.untracked.timeout`                   | `*`      | `string` | `21600`  |         |     |     | Time in seconds after which a credential request is considered failed if its credentials have not arrived, and its status cannot be queried, because the Issuer did not report its issuance process ID                                                                                                                                                             |
| `edc.iam.credential.request.tracked.timeout`                     | `*`      | `string` | `86400`  |         |     |     | Time in seconds after which a credential request is considered failed if its credentials have not arrived, although the Issuer keeps reporting it as pending                                                                                                                                                                                                       |
| `edc.iam.credential.request.state-machine.iteration-wait-millis` | `*`      | `string` | `1000`   |         |     |     | The iteration wait time in milliseconds in the state machine.                                                                                                                                                                                                                                                                                                      |
| `edc.iam.credential.request.state-machine.batch-size`            | `*`      | `string` | `10`     |         |     |     | The number of entities to be processed on every iteration. They are leased together, so the lease must outlast the processing of all of them.                                                                                                                                                                                                                      |
| `edc.iam.credential.request.state-machine.lease-duration-millis` | `*`      | `string` | `300000` |         |     |     | How long the entities of a batch are leased, in milliseconds. It must be longer than the processing of a whole batch, i.e. longer than the batch size times the call timeout, otherwise another runtime may process the same entities.                                                                                                                             |
| `edc.iam.credential.request.send.retry.limit`                    | `*`      | `string` | `80`     |         |     |     | How many times a failed operation is retried before it fails with an error. With the default delays, the retries take about six hours.                                                                                                                                                                                                                             |
| `edc.iam.credential.request.send.retry.base-delay.ms`            | `*`      | `string` | `1000`   |         |     |     | The base delay of retries in milliseconds. The n-th retry waits the base delay times 2 to the power of n, but at most the maximum delay.                                                                                                                                                                                                                           |
| `edc.iam.credential.request.send.retry.max-delay.ms`             | `*`      | `string` | `300000` |         |     |     | The maximum delay between two retries in milliseconds.                                                                                                                                                                                                                                                                                                             |
| `edc.iam.credential.request.send.connect-timeout.ms`             | `*`      | `string` | `5000`   |         |     |     | How long it may take to connect to the counterparty when a message is sent, in milliseconds.                                                                                                                                                                                                                                                                       |
| `edc.iam.credential.request.send.call-timeout.ms`                | `*`      | `string` | `15000`  |         |     |     | How long it may take to send a message and receive the response, in milliseconds. A message that times out is not retried right away, but by the state machine, like any other failure.                                                                                                                                                                            |
| `edc.iam.presentation.validity`                                  | `*`      | `string` | `60`     |         |     |     | How long a Verifiable Presentation is valid after it was created, in seconds. Determines the 'exp' claim of JWT presentations and the proof 'expires' of Linked-Data presentations.                                                                                                                                                                                |
| `edc.iam.presentation.vcdm20.plain.enabled`                      | `*`      | `string` | `false`  |         |     |     | Returns VCDM 2.0 presentations as the VP JWT itself, as defined by VC-JOSE-COSE, instead of wrapping it in an 'EnvelopedVerifiablePresentation' that is signed again as a JWT. Only enable this if all verifiers accept plain VP JWTs.                                                                                                                             |
| `edc.iam.accesstoken.publickey.fallback.enabled`                 | `*`      | `string` | `false`  |         |     |     | Verifies access tokens with keys from the vault or 'edc.iam.publickeys' if the participant context has no matching key pair. That lookup is not scoped to the participant context, so this should only be enabled for keys that are not managed as key pairs.                                                                                                      |
| `edc.iam.credential.delivery.reject.unverified`                  | `*`      | `string` | `false`  |         |     |     | Rejects delivered credentials in formats whose proof cannot be verified, such as credentials with an embedded Linked-Data proof. If disabled, they are stored with a warning.                                                                                                                                                                                      |
| `edc.iam.issuer.endpoint.restrict`                               | `*`      | `string` | `false`  |         |     |     | Whether requests to an Issuer's IssuerService endpoint, which is taken from the Issuer's DID document, must use HTTPS and may only be sent to hosts that resolve to public addresses, and whether redirects are not followed. Should be enabled if participants or Issuers are not trusted to only publish endpoints of legitimate Issuers in their DID documents. |
| `edc.iam.issuer.endpoint.allowed.hosts`                          |          | `string` | ``       |         |     |     | Comma-separated list of hosts (host names or IP addresses) that requests to an Issuer's IssuerService endpoint may be sent to, although they use plain HTTP or resolve to loopback, link-local or private addresses, e.g. an Issuer running in the same cluster. Only has an effect if 'edc.iam.issuer.endpoint.restrict' is enabled.                              |

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
- `org.eclipse.edc.iam.decentralizedclaims.spi.verification.SignatureSuiteRegistry` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.token.spi.TokenValidationService` (required)
- `org.eclipse.edc.token.spi.TokenValidationRulesRegistry` (required)
- `org.eclipse.edc.keys.spi.KeyParserRegistry` (required)
- `org.eclipse.edc.iam.decentralizedclaims.spi.verification.SignatureSuiteRegistry` (required)
- `org.eclipse.edc.identityhub.spi.keypair.KeyPairService` (required)
- `org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry` (required)
- `org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore` (required)
- `org.eclipse.edc.keys.spi.LocalPublicKeyService` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService` (required)
- `org.eclipse.edc.jwt.spi.signer.JwsSignerProvider` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry` (required)
- `org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore` (required)
- `org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService` (required)
- `okhttp3.OkHttpClient` (required)
- `org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore` (required)
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `org.eclipse.edc.spi.system.ExecutorInstrumentation` (required)

Module `identity-hub-credentials-store-sql`
-------------------------------------------
**Artifact:** org.eclipse.edc:identity-hub-credentials-store-sql:1.0.0-SNAPSHOT

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
**Artifact:** org.eclipse.edc:identity-hub-did:1.0.0-SNAPSHOT

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
- `org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore` (required)
- `org.eclipse.edc.spi.telemetry.Telemetry` (required)

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
**Artifact:** org.eclipse.edc:identity-hub-did-store-sql:1.0.0-SNAPSHOT

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
**Artifact:** org.eclipse.edc:identity-hub-keypair-store-sql:1.0.0-SNAPSHOT

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
**Artifact:** org.eclipse.edc:identity-hub-keypairs:1.0.0-SNAPSHOT

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
- `org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore` (required)

Module `identity-hub-keypairs-transit`
--------------------------------------
**Artifact:** org.eclipse.edc:identity-hub-keypairs-transit:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.keypairs.TransitKeyPairServiceExtension`
**Name:** "Hashicorp Transit KeyPair Service Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.identityhub.spi.keypair.KeyPairService`

#### Referenced (injected) services
- `org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore` (required)
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore` (required)
- `org.eclipse.edc.identityhub.spi.keypair.events.KeyPairObservable` (required)
- `org.eclipse.edc.identityhub.transit.TransitEngine` (required)
- `java.time.Clock` (required)

#### Class: `org.eclipse.edc.identityhub.TransitSecurityExtension`
**Name:** "Hashicorp Transit Security Extension"

**Overview:** No overview provided.


### Configuration

| Key                       | Required | Type     | Default | Pattern | Min | Max | Description                    |
| ------------------------- | -------- | -------- | ------- | ------- | --- | --- | ------------------------------ |
| `edc.vault.hashicorp.url` | `*`      | `string` | ``      |         |     |     | The URL of the Hashicorp Vault |

#### Provided services
- `org.eclipse.edc.jwt.spi.signer.JwsSignerProvider`
- `org.eclipse.edc.identityhub.transit.TransitEngine`

#### Referenced (injected) services
- `org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProviderFactory` (required)
- `org.eclipse.edc.http.spi.EdcHttpClient` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)

Module `identity-hub-participants`
----------------------------------
**Artifact:** org.eclipse.edc:identity-hub-participants:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.participantcontext.ParticipantContextExtension`
**Name:** "ParticipantContext Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService`
- `org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextObservable`

#### Referenced (injected) services
- `org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore` (required)
- `org.eclipse.edc.spi.security.Vault` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `org.eclipse.edc.identityhub.spi.did.store.DidResourceStore` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.StsAccountProvisioner` (required)
- `org.eclipse.edc.participantcontext.spi.config.service.ParticipantContextConfigService` (required)
- `org.eclipse.edc.spi.telemetry.Telemetry` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)

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
- `org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService` (required)
- `org.eclipse.edc.spi.telemetry.Telemetry` (required)

Module `identityhub-api-authentication`
---------------------------------------
**Artifact:** org.eclipse.edc:identityhub-api-authentication:1.0.0-SNAPSHOT

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
- `org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService` (required)
- `org.eclipse.edc.spi.security.Vault` (required)

Module `identityhub-api-authorization`
--------------------------------------
**Artifact:** org.eclipse.edc:identityhub-api-authorization:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.api.ApiAuthorizationExtension`
**Name:** "Identity API Authorization Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.api.auth.spi.AuthorizationService`

#### Referenced (injected) services
_None_

Module `issuance-process-api`
-----------------------------
**Artifact:** org.eclipse.edc:issuance-process-api:1.0.0-SNAPSHOT

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
- `org.eclipse.edc.api.auth.spi.AuthorizationService` (required)

Module `issuance-process-store-sql`
-----------------------------------
**Artifact:** org.eclipse.edc:issuance-process-store-sql:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.issuerservice.store.sql.issuanceprocess.SqlIssuanceProcessStoreExtension`
**Name:** "Issuance Process SQL Store Extension"

**Overview:** No overview provided.


### Configuration

| Key                                                       | Required | Type     | Default   | Pattern | Min | Max | Description                                                                                                                                                                                                                            |
| --------------------------------------------------------- | -------- | -------- | --------- | ------- | --- | --- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `edc.issuer.issuance.state-machine.iteration-wait-millis` | `*`      | `string` | `1000`    |         |     |     | The iteration wait time in milliseconds in the state machine.                                                                                                                                                                          |
| `edc.issuer.issuance.state-machine.batch-size`            | `*`      | `string` | `10`      |         |     |     | The number of entities to be processed on every iteration. They are leased together, so the lease must outlast the processing of all of them.                                                                                          |
| `edc.issuer.issuance.state-machine.lease-duration-millis` | `*`      | `string` | `300000`  |         |     |     | How long the entities of a batch are leased, in milliseconds. It must be longer than the processing of a whole batch, i.e. longer than the batch size times the call timeout, otherwise another runtime may process the same entities. |
| `edc.issuer.issuance.send.retry.limit`                    | `*`      | `string` | `80`      |         |     |     | How many times a failed operation is retried before it fails with an error. With the default delays, the retries take about six hours.                                                                                                 |
| `edc.issuer.issuance.send.retry.base-delay.ms`            | `*`      | `string` | `1000`    |         |     |     | The base delay of retries in milliseconds. The n-th retry waits the base delay times 2 to the power of n, but at most the maximum delay.                                                                                               |
| `edc.issuer.issuance.send.retry.max-delay.ms`             | `*`      | `string` | `300000`  |         |     |     | The maximum delay between two retries in milliseconds.                                                                                                                                                                                 |
| `edc.issuer.issuance.send.connect-timeout.ms`             | `*`      | `string` | `5000`    |         |     |     | How long it may take to connect to the counterparty when a message is sent, in milliseconds.                                                                                                                                           |
| `edc.issuer.issuance.send.call-timeout.ms`                | `*`      | `string` | `15000`   |         |     |     | How long it may take to send a message and receive the response, in milliseconds. A message that times out is not retried right away, but by the state machine, like any other failure.                                                |
| `edc.sql.store.issuanceprocess.datasource`                | `*`      | `string` | `default` |         |     |     | The datasource to be used                                                                                                                                                                                                              |

#### Provided services
- `org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.issuerservice.store.sql.issuanceprocess.IssuanceProcessStoreStatements` (optional)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)
- `java.time.Clock` (required)

Module `issuer-admin-api-authentication`
----------------------------------------
**Artifact:** org.eclipse.edc:issuer-admin-api-authentication:1.0.0-SNAPSHOT

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
- `org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService` (required)
- `org.eclipse.edc.spi.security.Vault` (required)

Module `issuer-admin-api-authentication-oauth2`
-----------------------------------------------
**Artifact:** org.eclipse.edc:issuer-admin-api-authentication-oauth2:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.issuerservice.api.Oauth2JwtAuthenticationExtension`
**Name:** "Issuer Admin API OAuth2/JWT Authentication Extension"

**Overview:** No overview provided.


### Configuration

| Key                                  | Required | Type     | Default  | Pattern | Min | Max | Description                                                                                                                              |
| ------------------------------------ | -------- | -------- | -------- | ------- | --- | --- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `edc.iam.oauth2.issuer`              |          | `string` | ``       |         |     |     | Expected value for the 'iss' claim for tokens coming from the OAuth2 server. If this is omitted, the 'iss' claim is not validated        |
| `edc.iam.oauth2.audience`            |          | `string` | ``       |         |     |     | Audience that the 'aud' claim of tokens coming from the OAuth2 server must contain. If this is omitted, the 'aud' claim is not validated |
| `edc.iam.oauth2.jwks.url`            | `*`      | `string` | ``       |         |     |     | Absolute URL where the JWKS of the OAuth2 server is hosted                                                                               |
| `edc.iam.oauth2.jwks.cache.validity` | `*`      | `string` | `300000` |         |     |     | Time (in ms) that cached JWKS are cached                                                                                                 |
| `edc.iam.oauth2.validity.leeway`     | `*`      | `string` | `5`      |         |     |     | Leeway (in sec) to allow when validating token time-based claims                                                                         |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.participantcontext.spi.service.ParticipantContextService` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.token.spi.TokenValidationService` (required)
- `org.eclipse.edc.keys.spi.KeyParserRegistry` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)

Module `issuer-admin-api-authorization-oauth2`
----------------------------------------------
**Artifact:** org.eclipse.edc:issuer-admin-api-authorization-oauth2:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.issuerservice.api.Oauth2AuthorizationExtension`
**Name:** "Issuer Admin API OAuth2 Authorization Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.api.auth.spi.AuthorizationService`

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)

Module `issuer-admin-api-configuration`
---------------------------------------
**Artifact:** org.eclipse.edc:issuer-admin-api-configuration:1.0.0-SNAPSHOT

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
- `org.eclipse.edc.api.auth.spi.AuthorizationService`

#### Referenced (injected) services
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.web.spi.configuration.PortMappingRegistry` (required)
- `org.eclipse.edc.spi.system.apiversion.ApiVersionService` (required)
- `org.eclipse.edc.web.spi.WebService` (required)

Module `issuerservice-attestation-definition-store-sql`
-------------------------------------------------------
**Artifact:** org.eclipse.edc:issuerservice-attestation-definition-store-sql:1.0.0-SNAPSHOT

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

Module `issuerservice-core`
---------------------------
**Artifact:** org.eclipse.edc:issuerservice-core:1.0.0-SNAPSHOT

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
**Artifact:** org.eclipse.edc:issuerservice-credential-definition-store-sql:1.0.0-SNAPSHOT

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
**Artifact:** org.eclipse.edc:issuerservice-credentials:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.issuerservice.credentials.CredentialDefaultServiceExtension`
**Name:** "Issuer Service Credential Default Services"

**Overview:** No overview provided.


### Configuration

| Key                                       | Required | Type     | Default    | Pattern | Min | Max | Description                                                                                                                      |
| ----------------------------------------- | -------- | -------- | ---------- | ------- | --- | --- | -------------------------------------------------------------------------------------------------------------------------------- |
| `edc.issuer.statuslist.bitstring.size`    | `*`      | `string` | `131072`   |         |     |     | The number of entries of a status list. A lower value than the minimum of the W3C Bitstring Status List, 131072, is raised to it |
| `edc.issuer.statuslist.validity`          | `*`      | `string` | `31536000` |         |     |     | How long a status list credential is valid after it was signed, in seconds                                                       |
| `edc.issuer.statuslist.renewal.period`    | `*`      | `string` | `86400`    |         |     |     | How often status list credentials that are about to expire are renewed, in seconds. 0 or less deactivates the renewal            |
| `edc.issuer.statuslist.renewal.threshold` | `*`      | `string` | `2592000`  |         |     |     | How long before it expires a status list credential is renewed, in seconds                                                       |

#### Provided services
- `org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListInfoFactoryRegistry`
- `org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListManager`

#### Referenced (injected) services
- `org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListCredentialPublisher` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.system.ExecutorInstrumentation` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerProfile` (required)

#### Class: `org.eclipse.edc.issuerservice.credentials.CredentialServiceExtension`
**Name:** "Issuer Service Credential Service"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.issuerservice.spi.credentials.CredentialStatusService`
- `org.eclipse.edc.issuerservice.spi.credentials.IssuerCredentialOfferService`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore` (required)
- `org.eclipse.edc.issuerservice.spi.holder.store.HolderStore` (required)
- `org.eclipse.edc.iam.decentralizedclaims.spi.CredentialServiceUrlResolver` (required)
- `org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService` (required)
- `org.eclipse.edc.http.spi.EdcHttpClient` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerMetadataService` (required)
- `org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListInfoFactoryRegistry` (required)
- `org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListManager` (required)
- `org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListCredentialPublisher` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `java.time.Clock` (required)

Module `issuerservice-database-attestations`
--------------------------------------------
**Artifact:** org.eclipse.edc:issuerservice-database-attestations:1.0.0-SNAPSHOT

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
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)

Module `issuerservice-holder-attestations`
------------------------------------------
**Artifact:** org.eclipse.edc:issuerservice-holder-attestations:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.issuerservice.issuance.attestation.holder.HolderAttestationExtension`
**Name:** "Holder Attestation"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactoryRegistry` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionValidatorRegistry` (required)

Module `issuerservice-holder-store-sql`
---------------------------------------
**Artifact:** org.eclipse.edc:issuerservice-holder-store-sql:1.0.0-SNAPSHOT

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
**Artifact:** org.eclipse.edc:issuerservice-holders:1.0.0-SNAPSHOT

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
**Artifact:** org.eclipse.edc:issuerservice-issuance:1.0.0-SNAPSHOT

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
- `org.eclipse.edc.issuerservice.spi.issuance.events.IssuanceObservable`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.store.CredentialDefinitionStore` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionStore` (required)
- `org.eclipse.edc.identityhub.spi.keypair.KeyPairService` (required)
- `org.eclipse.edc.jwt.spi.signer.JwsSignerProvider` (required)
- `org.eclipse.edc.issuerservice.spi.holder.HolderService` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService` (required)
- `org.eclipse.edc.spi.event.EventRouter` (required)

#### Class: `org.eclipse.edc.issuerservice.issuance.IssuanceCoreExtension`
**Name:** "Issuance Core Extension"

**Overview:** No overview provided.


### Configuration

| Key                                                       | Required | Type     | Default  | Pattern | Min | Max | Description                                                                                                                                                                                                                            |
| --------------------------------------------------------- | -------- | -------- | -------- | ------- | --- | --- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `edc.issuer.issuance.state-machine.iteration-wait-millis` | `*`      | `string` | `1000`   |         |     |     | The iteration wait time in milliseconds in the state machine.                                                                                                                                                                          |
| `edc.issuer.issuance.state-machine.batch-size`            | `*`      | `string` | `10`     |         |     |     | The number of entities to be processed on every iteration. They are leased together, so the lease must outlast the processing of all of them.                                                                                          |
| `edc.issuer.issuance.state-machine.lease-duration-millis` | `*`      | `string` | `300000` |         |     |     | How long the entities of a batch are leased, in milliseconds. It must be longer than the processing of a whole batch, i.e. longer than the batch size times the call timeout, otherwise another runtime may process the same entities. |
| `edc.issuer.issuance.send.retry.limit`                    | `*`      | `string` | `80`     |         |     |     | How many times a failed operation is retried before it fails with an error. With the default delays, the retries take about six hours.                                                                                                 |
| `edc.issuer.issuance.send.retry.base-delay.ms`            | `*`      | `string` | `1000`   |         |     |     | The base delay of retries in milliseconds. The n-th retry waits the base delay times 2 to the power of n, but at most the maximum delay.                                                                                               |
| `edc.issuer.issuance.send.retry.max-delay.ms`             | `*`      | `string` | `300000` |         |     |     | The maximum delay between two retries in milliseconds.                                                                                                                                                                                 |
| `edc.issuer.issuance.send.connect-timeout.ms`             | `*`      | `string` | `5000`   |         |     |     | How long it may take to connect to the counterparty when a message is sent, in milliseconds.                                                                                                                                           |
| `edc.issuer.issuance.send.call-timeout.ms`                | `*`      | `string` | `15000`  |         |     |     | How long it may take to send a message and receive the response, in milliseconds. A message that times out is not retried right away, but by the state machine, like any other failure.                                                |

#### Provided services
- `org.eclipse.edc.issuerservice.spi.issuance.process.IssuanceProcessManager`
- `org.eclipse.edc.issuerservice.spi.issuance.process.IssuanceProcessService`

#### Referenced (injected) services
- `org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.store.CredentialDefinitionStore` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore` (required)
- `org.eclipse.edc.spi.security.Vault` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.spi.telemetry.Telemetry` (required)
- `org.eclipse.edc.spi.system.ExecutorInstrumentation` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.process.retry.IssuanceProcessRetryStrategy` (optional)
- `org.eclipse.edc.issuerservice.spi.issuance.delivery.CredentialStorageClient` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.issuerservice.spi.credentials.CredentialStatusService` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.events.IssuanceObservable` (required)

Module `issuerservice-issuance-rules`
-------------------------------------
**Artifact:** org.eclipse.edc:issuerservice-issuance-rules:1.0.0-SNAPSHOT

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

Module `keypair-api`
--------------------
**Artifact:** org.eclipse.edc:keypair-api:1.0.0-SNAPSHOT

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
- `org.eclipse.edc.api.auth.spi.AuthorizationService` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)

Module `local-did-publisher`
----------------------------
**Artifact:** org.eclipse.edc:local-did-publisher:1.0.0-SNAPSHOT

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
**Artifact:** org.eclipse.edc:local-statuslist-publisher:1.0.0-SNAPSHOT

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
**Artifact:** org.eclipse.edc:participant-context-api:1.0.0-SNAPSHOT

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
- `org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService` (required)
- `org.eclipse.edc.api.auth.spi.AuthorizationService` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)

Module `presentation-api`
-------------------------
**Artifact:** org.eclipse.edc:presentation-api:1.0.0-SNAPSHOT

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

Module `storage-api`
--------------------
**Artifact:** org.eclipse.edc:storage-api:1.0.0-SNAPSHOT

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
- `org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService` (required)

Module `sts-account-provisioner`
--------------------------------
**Artifact:** org.eclipse.edc:sts-account-provisioner:1.0.0-SNAPSHOT

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
- `org.eclipse.edc.iam.decentralizedclaims.sts.spi.service.StsClientSecretGenerator` (optional)
- `org.eclipse.edc.iam.decentralizedclaims.sts.spi.service.StsAccountService` (required)
- `org.eclipse.edc.spi.telemetry.Telemetry` (required)

Module `sts-account-service-local`
----------------------------------
**Artifact:** org.eclipse.edc:sts-account-service-local:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.identityhub.sts.accountservice.StsAccountServiceExtension`
**Name:** "Local (embedded) STS Account Service Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.iam.decentralizedclaims.sts.spi.service.StsAccountService`

#### Referenced (injected) services
- `org.eclipse.edc.iam.decentralizedclaims.sts.spi.store.StsAccountStore` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.spi.security.Vault` (required)
- `org.eclipse.edc.iam.decentralizedclaims.sts.spi.service.StsClientSecretGenerator` (optional)

Module `sts-api`
----------------
**Artifact:** org.eclipse.edc:sts-api:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.api.iam.decentralizedclaims.sts.SecureTokenServiceApiExtension`
**Name:** "Secure Token Service API"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.iam.decentralizedclaims.sts.spi.service.StsAccountService` (required)
- `org.eclipse.edc.iam.decentralizedclaims.sts.spi.service.StsClientTokenGeneratorService` (required)
- `org.eclipse.edc.web.spi.WebService` (required)

#### Class: `org.eclipse.edc.api.iam.decentralizedclaims.sts.StsApiConfigurationExtension`
**Name:** "Secure Token Service API configuration"

**Overview:** No overview provided.


### Configuration

| Key                 | Required | Type     | Default    | Pattern | Min | Max | Description                                                                                                            |
| ------------------- | -------- | -------- | ---------- | ------- | --- | --- | ---------------------------------------------------------------------------------------------------------------------- |
| `web.http.sts.port` | `*`      | `string` | `9292`     |         |     |     | Port for sts api context                                                                                               |
| `web.http.sts.path` | `*`      | `string` | `/api/sts` |         |     |     | Path for sts api context                                                                                               |
| `web.http.sts.host` |          | `string` | ``         |         |     |     | Host name or IP address of the network interface the sts api context listens on. Listens on all interfaces if not set. |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.configuration.PortMappingRegistry` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.spi.system.apiversion.ApiVersionService` (required)
- `org.eclipse.edc.web.jetty.JettyService` (optional)

Module `sts-client-store-sql`
-----------------------------
**Artifact:** org.eclipse.edc:sts-client-store-sql:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.iam.decentralizedclaims.sts.store.SqlStsClientStoreExtension`
**Name:** "SQL sts accounts store"

**Overview:** No overview provided.


### Configuration

| Key                                  | Required | Type     | Default   | Pattern | Min | Max | Description               |
| ------------------------------------ | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.stsclient.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.iam.decentralizedclaims.sts.spi.store.StsAccountStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.iam.decentralizedclaims.sts.store.schema.StsClientStatements` (optional)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)

Module `sts-core`
-----------------
**Artifact:** org.eclipse.edc:sts-core:1.0.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.iam.decentralizedclaims.sts.defaults.StsDefaultServicesExtension`
**Name:** "Secure Token Service Default Services"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.iam.decentralizedclaims.sts.spi.store.StsAccountStore`

#### Referenced (injected) services
- `org.eclipse.edc.spi.query.CriterionOperatorRegistry` (required)

#### Class: `org.eclipse.edc.iam.decentralizedclaims.sts.StsClientTokenGeneratorExtension`
**Name:** "STS Client Token Generator Extension"

**Overview:** No overview provided.


### Configuration

| Key                            | Required | Type     | Default | Pattern | Min | Max | Description                                                         |
| ------------------------------ | -------- | -------- | ------- | ------- | --- | --- | ------------------------------------------------------------------- |
| `edc.iam.sts.token.expiration` | `*`      | `string` | `5`     |         |     |     | Self-issued ID Token expiration in minutes. By default is 5 minutes |

#### Provided services
- `org.eclipse.edc.iam.decentralizedclaims.sts.spi.service.StsClientTokenGeneratorService`

#### Referenced (injected) services
- `org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService` (required)

#### Class: `org.eclipse.edc.iam.decentralizedclaims.sts.EmbeddedStsServiceExtension`
**Name:** "Local (embedded) STS Account Service Extension"

**Overview:** No overview provided.


### Configuration

| Key                            | Required | Type     | Default | Pattern | Min | Max | Description                                                         |
| ------------------------------ | -------- | -------- | ------- | ------- | --- | --- | ------------------------------------------------------------------- |
| `edc.iam.sts.token.expiration` | `*`      | `string` | `5`     |         |     |     | Self-issued ID Token expiration in minutes. By default is 5 minutes |

#### Provided services
- `org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService`

#### Referenced (injected) services
- `java.time.Clock` (required)
- `org.eclipse.edc.jwt.spi.signer.JwsSignerProvider` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.identityhub.spi.keypair.KeyPairService` (required)
- `org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService` (required)

Module `test-attestations`
--------------------------
**Artifact:** org.eclipse.edc:test-attestations:1.0.0-SNAPSHOT

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
**Artifact:** org.eclipse.edc:verifiable-credentials-api:1.0.0-SNAPSHOT

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
- `org.eclipse.edc.api.auth.spi.AuthorizationService` (required)
- `org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestManager` (required)
- `org.eclipse.edc.identityhub.spi.transformation.DiscriminatorMappingRegistry` (required)

