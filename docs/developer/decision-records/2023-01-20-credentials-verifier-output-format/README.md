# Identity Hub - Credentials Verifier claims format

## Decision

The object returned in output of the `IdentityHubCredentialsVerifier` will be changed to match the [W3C Credentials](https://www.w3.org/TR/vc-data-model/#credentials).

## Rationale

Current implementation of the `IdentityHubCredentialsVerifier` such as described [here](../2022-07-01-get-claims/README.md)
returns verified Credentials under the format of JWT claims, with the `vc` claim containing the actual Credential subject claims.
This approach was relevant as long as the Identity Hub was only supporting Verifiable Credentials (VC) under the format of signed JWT.
However, usually VCs are represented under the `application/vc+ldp` format, which uses an embedded proof instead of relying on a signed JWT.
Also, this approach is cumbersome for the policy evaluation.

It is chosen to introduce a `Credential` java object whose shape is compliant with the [W3C Specification](https://www.w3.org/TR/vc-data-model/#credentials)
and that will be returned in output of the `IdentityHubCredentialsVerifier` instead of the JWT claims.

## Approach

A java POJO called `Credential` and following the [W3C Specification](https://www.w3.org/TR/vc-data-model/#credentials) will be introduced
in the `spi` package and returned in output of the `IdentityHubCredentialsVerifier`.