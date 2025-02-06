# Credential Issuance Concepts and Architecture

> This document assumes a solid understanding of
> the  [Decentralized Claims Protocol](https://github.com/eclipse-dataspace-dcp/decentralized-claims-protocol).

Credential issuance involves receiving a request by a holder and, if approved, generating one or more Verifiable
Credentials (VCs). This document defines the issuance process architecture and how it will be implemented.

## Concepts

### Onboarding, Issuance, and Re-Issuance

It is important to differentiate onboarding and issuance. Onboarding is the process where a company signs up to become a
dataspace member. This may involve, among other things, proving the company is an active legal entity and executing
legally binding agreements. The output of the onboarding process is a set of *attestations*: Company X has fulfilled its
obligations to be a member of Dataspace Z. Issuance is the process where a Verifiable Credential is created and
delivered to the holder's Credential Service (e.g. Identity Hub). The holder always initiates issuance via its
Credential Service using a protocol such as DCP. Issuance has no special knowledge about onboarding, and the two
processes are decoupled. This may seem counter-intuitive, so let's review how this is the case.

All issuance processes require one or more attestation inputs. Attestations may come from onboarding or another process.
For example, Dataspace Z may offer a new credential type that requires an additional legal agreement. The issuance
process for the new credential type requires an attestation that a contract was signed. This is the same sequence as
onboarding. Hence, onboarding is only a special case of acquiring a set of input attestations for issuance.

In this architecture, there is no difference between issuance and re-issuance. Take the case when a membership
credential needs to be renewed. The company's Credential Service initiates the issuance process again, and the Issuer
Service checks for a valid attestation and generates a new credential.

### Attestations and Trust Channels

Attestation inputs require a trust channel from the Issuer Service to the source of the attestation. Let's examine this
in the context of onboarding. The onboarding process must output the following attestations:

- A company is a valid Dataspace Z member.
- A company has passed an optional enhanced security audit.

This list can be represented as an abstract table where each row consists of a DID and a set of 0..N attributes (
name/value pairs). The onboarding process can insert rows in the table but not modify or delete them. Other processes
may modify or delete rows, for example, in the new credential scenario outlined previously or in the case of membership
expiration.

The Issuer Services uses the abstract table as an attestation source. This is a trust channel between the onboarding
provider and the Issuer Service. Now, let's expand the example to consider a multi-issuer scenario. In this scenario, we
do not want to establish a direct trust channel. Let's assume that Issuer X will issue credentials to all Dataspace Z
members. This can be modeled as:

Onboarding --table attestation--> Membership Credential Issuance --> Membership VC as attestation --> Credential X
Issuance

In this case, the attestation source for Credential X issuance is the Dataspace Z Membership credential. This can be
achieved using the standard DCP issuance flow.

### Issuance Scenarios

The following issuance scenarios will be supported.

#### Initial Credential Issuance

The onboarding process verifies the company, associates a DID, and gathers relevant attributes. The company
Credential Service (Identity Hub) initiates an issuance request to the issuer service, including a Self-Issued ID Token
as per the DCP specification. The Self-Issued ID Token proves the requester (Credential Service) controls the DID
associated with the company. The Issuer Service can look up the company's attributes from the abstract table and issue
the appropriate credentials.

#### Re-Issuance/Renewal

As previously noted, re-issuance works the same as issuance. The Credential Service initiates a request, including the
Self-Issued ID Token, and the Issuer Service looks up the DID in the abstract table.

Note that the simplicity of this process allows for automation. Credentials can have short expiration periods (e.g.,
weeks or months). A Credential Service can monitor held credentials and initiate a renewal request when an expiration
period is approached.

#### Additional Credential Issuance

Consider the case where a new credential is offered by Dataspace Z that requires signing a new legal agreement.
Instead of mandating the member go through onboarding again, a streamlined signature process can update the abstract
table. At that point, the Credential Service can initiate an issuance request for the new credential.

## Architecture

Our exemplary use case will be the dataspace onboarding process described above:

- An organization applies to become a member of a dataspace. The dataspace issuer can grant two credential types, a
  `MembershipCredential` and an optional `DataAuditCredential` for those organizations that have completed an
  independent compliance audit.
- In our example, the onboarding authority and issuer are the same. The organization signs up with the onboarding
  authority, uploads required documents, and the authority then verifies the organization.
- After the organization is verified, the onboarding authority asks the former to supply its `DID`.
- At that point, the organization can initiate an issuance request using a Self-Signed Identity Token per the DCP
  specification.

Several technical requirements are worth noting:

- Credential issuance may be bound to different protocols. The Issuer Service will support DCP, but it will be possible
  to support other protocols in the future if required. Multi-protocol support will also enable the Issuer Service to
  support multiple DCP versions.
- Credentials may be requested and issued in batch.
- It must be possible to configure new credential types and the process for issuing them.
- Credential Issuance must support reliable delivery of generated credentials.
- Credential Issuance must guard against DDoS attacks.

### Credential Definition

The first step is to introduce credential definitions for each credential type. A credential definition configures a
credential type and requirements for issuance:

```json
{
  "credentialType": "MembershipCredential",
  "dataModel": "1.1",
  "schema": "",
  "validityPeriod": 1000000,
  "format": "",
  "attestations": [
    "onboarding"
  ],
  "rules": [
    {
      "type": "expression",
      "configuration": {
        "claim": "onboarding.signedDocuments",
        "operator": "eq",
        "value": true
      }
    }
  ],
  "mappings": [
    {
      "input": "companyNumber",
      "output": "credentialSubject.companyNumber",
      "required": true
    },
    {
      "input": "expiration",
      "output": "expirationDate"
    }
  ]
}
```

This is deserialized to:

```java
public class CredentialDefinition {
    private String credentialType;
    private String schema;
    private String format;
    private long validity;

    private DataModelVersion dataModel = DataModelVersion.V_1_1;

    private List<String> attestations = new ArrayList<>();
    private List<CredentialRuleDefinition> rules = new ArrayList<>();
    private final List<MappingDefinition> mappings = new ArrayList<>();
}
```

The following properties are defined:

- `credentialType`: The credential type, e.g. `MembershipCredential`
- `schema`: The unparsed credential Json Schema
- `validity`: An optional validity defining the length of time the credential should be valid for in milliseconds.
- `dataModel`: The VC Data Model to use.
- `attestations`: The attestation sources that will be executed for an issuance request.
- `rules`: The rules that will be evaluated to determine if a credential should be issued.
- `mappingDefinitions`: How data is mapped into the issued credential.

### Request Processing

When a holder issuance request is received, processing is divided into two stages. The first stage synchronously
verifies the request, enqueues it, and sends an ack back to the client. If verification fails, processing is aborted and
an HTTP 401 error is returned to the client. The second step asynchronously approves the request, generates the
verifiable credentials and sends them to the client's Credential Service.

#### Verification Step 1: Attestations

The verification process evaluates the set of attestations configured for all credential definitions. Attestations
represent claims that are required to issue a credential. Returning to our example, the `MembershipCredential` type
requires an attestation from the onboarding system that a particular `DID` is a member of the dataspace. Attestations
are configured using an `AttestationDefinition`:

```java
public record AttestationDefinition(String id, String attestationType, Map<String, Object> configuration) {
}
```

Attestation definitions have a unique ID, a type, and a configuration. Attestation types are extensible and can be
a verifiable presentation (for example, obtained using the DCP Presentation Flow), a database entry, or some other
source. At runtime, an `AttestationSource` is responsible for resolving an attestation for a given definition.
Custom attestation types are created by registering an `AttestationSourceFactory` in the
`AttestationSourceFactoryRegistry`:

```java
public interface AttestationSourceFactoryRegistry {

    Set<String> registeredTypes();

    void registerFactory(AttestationSourceFactory factory);

}
```

An `AttestationSourceFactory` implementation creates an `AttestationSource` given an `AttestationDefinition`:

```java
public interface AttestationSourceFactory {

    String getType();

    Result<Void> validate(AttestationDefinition definition);

    AttestationSource createSource(AttestationDefinition definition);

}
```

The `AttestationSource` is responsible for returning attestation data, or claims, at runtime for a particular
`AttestationDefinition`:

```java
public interface AttestationSource {

    Result<Map<String, Object>> execute(AttestationContext context);

}
```

To see how this ties together, let's return to the `MembershipCredential`. The credential definition is configured to
require the following attestation definition:

```json
{
  "id": "onboarding",
  "attestationType": "database",
  "configuration": {
    "dataSource": "...",
    "table": "....",
    "outputClaim": "onboarding",
    "required": true
  }
}
```

The `onboarding` definition is a database attestation type, which means it sources its data from a table and places it
in the `onboarding` claims property of the issuance context (more on this later). The external onboarding process is
responsible for populating that table via an API. At runtime, this definition is passed to the database implementation
of the `AttestationSourceFactory`, which instantiates an `AttestationSource` responsible for resolving the data.

##### The `AttestationPipeline` Service

When an issuance request is received, the Issuer Service will collect all `CredentialDefinition`s and combine the
`attestations` into a set to eliminate duplicates. An `AttestationContext` will be created containing the holder
`DID` and any `ClaimTokens` associated with Verified Presentations (validated) gathered as part of the issuance
protocol. These will be passed to the `AttestationPipeline` for evaluation:

```java
public interface AttestationPipeline {

    Result<Map<String, Object>> evaluate(Set<String> attestations, AttestationContext context);

}
```

The pipeline returns the resolved claims or a failure if a required attestation is not present. This data is then
passed to the rules evaluation step.

#### Verification Step 2: Rules

The next synchronous step is to evaluate rules configured on the credential definition:

```json
{
  "rules": [
    {
      "type": "expression",
      "configuration": {
        "claim": "onboarding.signedDocuments",
        "operator": "eq",
        "value": true
      }
    }
  ]
}
```

A rule is deserialized to a `CredentialRuleDefinition`, which contains a type and configuration:

```java
public record CredentialRuleDefinition(String type, Map<String, Object> configuration) {
}
```

The `expression` rule type supports simple property evaluations. Rules are extensible and added to the
`CredentialRuleFactoryRegistry`:

```java
public interface CredentialRuleFactoryRegistry {

    Set<String> registeredTypes();

    void registerFactory(CredentialRuleFactory factory);

    CredentialRuleFactory resolveFactory(String type);

}
```

After attestations are sourced, the issuance process evaluates the configured rules for each credential definition. This
is done by creating and executing `CredentialRule`s for each `CredentialDefinition` using the appropriate
`CredentialRuleFactory`:

```java
public interface CredentialRuleFactory {

    String getType();

    Result<Void> validate(CredentialRuleDefinition definition);

    CredentialRule createRule(CredentialRuleDefinition definition);
}
```

The `CredentialRule` takes an `IssuanceContext` which contains the output of the attestation sourcing as `claims`:

```java
public interface CredentialRule {

    Result<Void> evaluate(IssuanceContext context);

}
```

If a rule does not evaluate successfully, processing is aborted and an HTTP 401 is returned to the client. If all rules
complete successfully, an `IssuanceProcess` is created:

```java
public class IssuanceProcess {
    public enum State {
        SUBMITTED, APPROVED, DELIVERED, ERRORED
    }

    private final State state = State.SUBMITTED;
    private long stateTimestamp;
    private int retries;
    private int errorCode;

    private long creationTime;

    private string tokenAlias;
    private Map<String, Object> claims;
    private List<String> credentialDefinitions;
}
```

The `ExternalApprovalPredicate` is called which returns `true` if the process requires external approval or `false` if
it is automatically approved:

```java
public interface ExternalApprovalPredicate extends Predicate<ExternalApprovalPredicate.ProcessRecord> {
    record ProcessRecord(String issuanceId, Set<String> credentialTypes) {
    }
}
```

The default implementation will always return `false`. If `false` is returned, the `IssuanceProcess` is placed in the
`APPROVED` state; otherwise, it remains in the `SUBMITTED` state. The `IssuanceProcess` is then persisted and an ack is
returned to the client.

> NOTE the existing `PendingGuard` feature is not used to avoid creating an additional persisted state.

#### Asynchronous Approval and Generation

An `IssuanceProcessManager` will periodically scan and process persisted `IssuanceProcess` instances.

##### The `SUBMITTED` State

The `IssuanceProcessManager` will ignore the `SUBMITTED` state as it is up to an external approval system to advance the
state.

##### The `APPROVED` State

The `IssuanceProcessManager` will individually process `credentialDefinitions` entries by executing `mappings` against
the persisted `claims` and feeding that data to a credential generation process. When all Verifiable Credentials have
been generated, delivery will be attempted to the holder's CredentialService. `CredentialResource`s will be
transacitonally saved to persistent storage as part of the delivery process. If successful, the `IssuanceProcess` will
be transitioned to the `DELIVERED` state.

If delivery is not successful, the transaction will be rolled back and generation will be tried again for a configured
time period and transitioned to `ERROR` if delivery is not successful.

##### The `DELIVERED` State

The `DELIVERED` state is terminal.

##### The `ERRORED` State

The `ERRORED` state is terminal.
