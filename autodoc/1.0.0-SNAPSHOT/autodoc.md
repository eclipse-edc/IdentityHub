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

| Key                                  | Required | Type     | Default | Pattern | Min | Max | Description                                                                                  |
| ------------------------------------ | -------- | -------- | ------- | ------- | --- | --- | -------------------------------------------------------------------------------------------- |
| `edc.iam.accesstoken.jti.validation` | `*`      | `string` | `false` |         |     |     | Activates the JTI check: access tokens can only be used once to guard against replay attacks |
| `edc.issuance.anonymous.allowed`     | `*`      | `string` | `false` |         |     |     | Allow anonymous onboarding                                                                   |

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
- `org.eclipse.edc.http.spi.EdcHttpClient` (required)
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

| Key                                  | Required | Type     | Default | Pattern | Min | Max | Description                                                                                  |
| ------------------------------------ | -------- | -------- | ------- | ------- | --- | --- | -------------------------------------------------------------------------------------------- |
| `edc.iam.accesstoken.jti.validation` | `*`      | `string` | `false` |         |     |     | Activates the JTI check: access tokens can only be used once to guard against replay attacks |
| `edc.issuance.anonymous.allowed`     | `*`      | `string` | `false` |         |     |     | Allow anonymous onboarding                                                                   |

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
- `org.eclipse.edc.http.spi.EdcHttpClient` (required)
- `org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService` (required)
- `org.eclipse.edc.spi.security.Vault` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry` (required)
- `org.eclipse.edc.identityhub.protocols.dcp.spi.DcpProfileRegistry` (required)
- `org.eclipse.edc.jwt.validation.jti.JtiValidationStore` (optional)
- `org.eclipse.edc.spi.telemetry.Telemetry` (required)
- `org.eclipse.edc.issuerservice.spi.issuance.events.IssuanceObservable` (required)

