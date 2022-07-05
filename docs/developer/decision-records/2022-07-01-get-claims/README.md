# Identity Hub - Get claims

This document explains how we will get the claims of a participant, in order to apply access policies.

## Context

When an EDC participant receives an IDS request, it must verify the caller's identity and apply access policies for the caller.
To apply policies, it needs to get the claims of the participant.
This document explains the mechanism to get the claims from IdentityHub.

## What is a claim

A claim is a statement about a subject. For example, a claim could be `ParticipantA` is in `region` `eu`.
See [this documentation](https://www.w3.org/TR/vc-data-model/#claims) for more details.

## Get claims

This document focuses on getting the claims of a Participant, assuming the participant's identity is already verified.
The participant will use the claims to apply the access policy.  
Each participant has an IdentityHub instance, exposing an endpoint to get its [Verifiable Credentials](https://www.w3.org/TR/vc-data-model/).
The IdentityHub endpoint returns a list of JWS containing a Verifiable Credential in their payload.
(see [example Verifiable Credential (as JWT)](https://www.w3.org/TR/vc-data-model/#example-usage-of-the-credentialsubject-property)).
The VerifiableCredentials contain [claims](https://www.w3.org/TR/vc-data-model/#claims) that can be extracted.

![Apply policy flow](apply-policies-flow.png)

Let's focus on the `Get claims` box.
`Participant B` wants to get the claims of `Participant A`, to apply access policy.
For example, Participant A could have the policy that Participant B should be claimed as a `participantOf`
`dataspaceA` by the issuer "gaia-x.com".

5. Participant B extracts the IdentityHub URL of participant A from a DID Document obtained in previous steps.
6. Participant B gets VerifiableCredentials from IdentityHub A.
7. Participant B gets a list of JWS. Each JWS contains a [Verifiable credential](https://www.w3.org/TR/vc-data-model/) in its payload, (see [example Verifiable Credential (as JWT)](https://www.w3.org/TR/vc-data-model/#example-usage-of-the-credentialsubject-property)).  
For each JWS:  
   8. Participant B parses the JWS, extracts the issuer DID URL from the JWS payload.  
   9. Participant B retrieves the issuer DID Document.  
   10. From the issuer DID Document, participant B extracts the public key of the issuer and uses it to verify the JWS signature.  
   11. If the signature is successfully verified, it extracts the claims of the participant, each claim needs to be associated with its issuer.  
12. The list of the claims will be used as input to apply the access policies.
