# Identity Hub - Self Description

This document explains how the Self-Description of a dataspace participant will be stored into the Identity Hub and retrieved from it.

## Context

Gaia-X defines a set of rules representing the minimum baseline to be part of a Gaia-X Ecosystem. This set of rules is the
[Gaia-X Trust Framework](https://gaia-x.gitlab.io/policy-rules-committee/trust-framework/), which is centered around the _Self-Description_ document.

Each dataspace participant (consumer, provider, federator) is represented by a Self-Description document.

According to [Gaia-X Architecture Document](https://gaia-x.eu/wp-content/uploads/2022/06/Gaia-x-Architecture-Document-22.04-Release.pdf),
"Self-Descriptions are W3C Verifiable Presentations in the JSON-LD format. Self-Description consist of a list of
Verifiable Credentials. Verifiable Credentials themselves contain a list of Claims: assertions about Entities
expressed in the [RDF](https://www.w3.org/RDF/) data model. Both Verifiable Credentials and Verifiable Presentations come with
cryptographic signatures to increase the level of trust".

Note that a Self-Description can aggregate Verifiable Credentials from different issuers, each having its own cryptographic signature.

An example of valid Self-Description can be found [here](https://gitlab.com/gaia-x/gaia-x-community/gx-hackathon/gx-hackathon-4/-/blob/2e2023a52d2850448c2b745e415ece481811de40/Example%20Self-Descriptions/participantEdc.txt), which corresponds to the one used during the Gaia-X Hackathon #4.

The reason to serve the Self-Description document via the Identity Hub is to enable the dynamic generation and signature (cf. Gaia-X [Trust Anchors](https://gaia-x.eu/wp-content/uploads/2022/05/Gaia-X-Trust-Framework-22.04.pdf)) of the Self-Description from the Identity Hub
at a later stage. As the Self-Description will already be served by the Identity Hub with this version, there will be no change for the user when Self-Description will be internally generated.

## Assumptions

- Self-Description document is static, i.e. no update of the Self-Description is supported in this version.
- Verifiable Credentials contained in the Self-Description are not served by the main Identity Hub `POST` endpoint.
  Thus, they are not verified nor evaluated by the [Policy Engine](../2022-07-01-get-claims/README.md). This will be addressed in a later version.
- No check is performed in current version to ensure that Self-Description is valid according to [Gaia-X Compliance API](https://compliance.gaia-x.eu/docs/#/).

## Implementation proposal

### Self-Description storage

The Self-Description will be loaded at startup from a static `.json` file and stored in-memory. After being loaded, the Self-Description is then passed
to the API controller which expose it through an endpoint.

### Self-Description endpoint

The Self-Description can be retrieved from the Identity Hub through a `GET` endpoint under the path `identity-hub/self-description`.

The base URL to access the Self-Description is the same as the one for the main Identity Hub REST API. Thus, the URL for accessing
the Self-Description document can be resolved from the same `serviceEndpoint` of the DID document.
