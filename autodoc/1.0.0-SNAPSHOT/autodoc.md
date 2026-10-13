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
| `edc.iam.accesstoken.jti.validation`                      | `*`      | `string` | `false`  |         |     |     | Activates the JTI check: access tokens can only be used once to guard against replay attacks                                                                                                                                           |
| `edc.issuance.anonymous.allowed`                          | `*`      | `string` | `false`  |         |     |     | Allow anonymous onboarding                                                                                                                                                                                                             |

#### Provided services
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
- `org.eclipse.edc.jwt.validation.jti.JtiValidationStore` (optional)
- `org.eclipse.edc.spi.telemetry.Telemetry` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.events.IssuanceObservable` (required)

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
| `edc.iam.accesstoken.jti.validation`                      | `*`      | `string` | `false`  |         |     |     | Activates the JTI check: access tokens can only be used once to guard against replay attacks                                                                                                                                           |
| `edc.issuance.anonymous.allowed`                          | `*`      | `string` | `false`  |         |     |     | Allow anonymous onboarding                                                                                                                                                                                                             |

#### Provided services
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
- `org.eclipse.edc.jwt.validation.jti.JtiValidationStore` (optional)
- `org.eclipse.edc.spi.telemetry.Telemetry` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.events.IssuanceObservable` (required)

