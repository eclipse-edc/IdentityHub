# The SecureTokenService must always be embedded in IdentityHub

## Decision

When using EDC modules, the SecureTokenService (STS) must always be embedded in the IdentityHub runtime. Running it in
standalone mode is not supported.

## Rationale

By definition, the STS and IdentityHub are coupled together, because the shape of the access token must be known to
either of them.

In addition, IdentityHub is designed to manage all security-related material such as key pairs, which is
bound to a participant context, and for which it contains specific APIs and services.

Our EDC components shall reflect this coupling by directly embedding the SecureTokenService into the IdentityHub
runtime.

## Approach

### Removal of the STS Accounts API

The fact that STS maintains an account (client ID and client secret) for each participant context becomes an
implementation detail that need not be exposed externally through a REST API.

When creating participant contexts, the IdentityHub always creates the "account" internally without the need for a
remote call.

This also removes the need for a `RemoteStsAccountService`, because IdentityHub can simply create a
local "account". This will further simplify the `StsAccountProvisioner -> StsAccountService` indirection.

### Moving code from the Connector to IdentityHub

Most of the STS modules that are currently located in EDC can be moved to the IdentityHub repository. The connector only
needs to have an `RemoteSecureTokenService` implementation with which it can communicate with STS/IdentityHub using
REST.

IdentityHub on the other hand, does not need any remote call code anymore, because it always has STS in the same
runtime.

_Note that embedding STS/IdentityHub in the connector runtime would still be possible, but is **not** a recommended
deployment scenario._

### Adapting MinimumViableDataspace

MVD will be modified (and simplified) accordingly.

## Further considerations

### What if

##### ... I absolutely want to run STS standalone?

In this case the standalone runtime must implement its own PKI based on DID documents. Specifically, the following
aspects would need to be implemented:

- key management: to rotate, revoke, add, ... key pairs
- DID management: public keys need to be added/updated/removed to/from the DID document so that verifiers can resolve them
- account mapping: participant contexts (in IdentityHub) must be mapped to STS accounts, so that IdentityHub can act _on
  behalf_ of the correct participant. This is particularly relevant when making credential issuance requests.

##### ... I need to use a third-party IdP to create tokens?

In cases where all security tokens must be created by a central (third-party) IdP such as KeyCloak, developing a plugin
for that IdP will most likely become necessary, so that it can create DCP-compliant SI-tokens. In all likeliness, an
extension must be developed in IdentityHub that maps participant contexts onto IdP user principals.

Note that in this scenario the IdP is responsible for key management and a DCP-compliant PKI.

Note also that neither of these deployment scenarios is recommended or supported by the EDC project.
