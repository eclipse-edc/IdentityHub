# Identity Hub - Self Description

This document explains how the Self-Description of a dataspace participant will be stored into the Identity Hub and retrieved from it.

## Context

Gaia-X defines a set of rules that define the minimum baseline to be part of a Gaia-X Ecosystem. This set of rules is the so-called
[Gaia-X Trust Framework](https://gaia-x.gitlab.io/policy-rules-committee/trust-framework/), which is centered around the _Self-Description_ document.

Each dataspace participant (consumer, provider, federator) is represented by a Self-Description document.

According to [spec](https://gaia-x.eu/wp-content/uploads/2022/06/Gaia-x-Architecture-Document-22.04-Release.pdf),
"Self-Descriptions are W3C Verifiable Presentations in the JSON-LD format. Self-Description consist of a list of
Verifiable Credentials. Verifiable Credentials themselves contain a list of Claims: assertions about Entities
expressed in the RDF data model. Both Verifiable Credentials and Verifiable Presentations come with
cryptographic signatures to increase the level of trust".

Note that a Self-Description can aggregate Verifiable Credentials from different issuers, each having its own cryptographic signature.

An example of valid Self-Description can be found [here](https://gitlab.com/gaia-x/gaia-x-community/gx-hackathon/gx-hackathon-4/-/blob/main/Example%20Self-Descriptions/participantA1digital.json), which corresponds to the one used during the Gaia-X Hackathon #4.

Long term, the Verifiable Credentials from the Self-Description should be stored into the Identity Hub so that they can latter
been verified and used for the policy enforcement (see [here](../2022-07-01-get-claims/README.md) for more details).

However, as specifications of the Self-Description documents are not definitive yet, it is for now preferred to restrict the usage of the
Self-Description document for display only. More specifically, we aim at exposing the Self-Description document through a dedicated REST endpoint,
which is resolved from the DID document of the participant.

## Assumptions

- Self-Description document is static, i.e. no update of the Self-Description is supported in this version
- Self-Description document is provided at startup , i.e. no need for an endpoint allowing to update the Self-Description.

## Implementation proposal

### Self-Description storage

The Self-Description JSON document must be stored in the Identity Hub so that it can latter been served by the endpoint.
A tentative interface for the `SelfDescriptionStore` would be:

```java
public class SelfDescriptionStore {

    void store(Object selfDescription);

    Object get();
}
```

An in-memory implementation of this store will also be provided.

The populated at startup of the Identity Hub by reading the Self-Description from a static JSON file.

### Self-Description endpoint

The Self-Description can be retrieved from the store through a REST `GET` endpoint under the path `identity-hub/self-description`.

The base URL to access the Self-Description is the same as the one for the main Identity Hub REST API. Thus, the URL for accessing
the Self-Description document can be resolved from the same `serviceEndpoint` of the DID document.
