# APIs of the IdentityHub

## Hub API

This refers to a group of endpoints that implement functionality which is defined in
the  [Decentralized Claims Protocol (DCP) specification](https://projects.eclipse.org/projects/technology.dataspace-dcp).
These APIs is intended to be exposed to the internet.

### Presentation API

This API allows clients to request credentials in the form of a VerifiablePresentation. It is part of the Verifiable
Credential Presentation protocol of the DCP specification.

Please refer to the [API documentation](https://eclipse-edc.github.io/IdentityHub/openapi/presentation-api) for more
details.

### Storage API

This API offers endpoints to credentials issuers to store newly issued credentials in IdentityHub's persistent storage.

> not yet implemented

## Identity API

IdentityHub's Identity API is used to manipulate information that pertain to a participant's identity. Specifically, DID
documents, VerifiableCredentials and key pairs. Authorized clients can CRUD that information, for example adding
a `service` to a DID document, or rotating or revoking a key pair.

All endpoints of this API require authorization - every participant is only allowed to modify their own information. For
details about IdentityHub's security and RBAC concept, please refer to [this document](./identity-api.security.md).

The Identity API is not intended to be exposed to the internet without additional network infrastructure, such as API
gateways. Please refer to
the [EDC Best Practices](https://github.com/eclipse-edc/docs/blob/main/developer/best-practices.md) for more
information.

For more information please refer to
the [API documentation](https://eclipse-edc.github.io/IdentityHub/openapi/identity-api).

> Please note that this API is explicitly _not_ intended to store or update VerifiableCredentials. This process is
> defined in the DCP specification and must be done via the Storage API.

## Observability API

The Observability API is intended to provide information about the application health to the Docker daemon via Docker
health checks and the Kubernetes control plane via Kubernetes Readiness Probes. It is not intended to be reachable
from outside the container as it lacks access control.