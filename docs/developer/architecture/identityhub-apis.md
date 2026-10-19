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

## Request body size limits

The DCP endpoints (Presentation API, Storage API and Credential Offer API on the `credentials` context, and the
Credential Request API on the `issuance` context of the IssuerService) reject requests whose body exceeds a maximum size
with `413 Payload Too Large`. The limit defaults to 5 MiB and can be configured per context:

| Setting                            | Default   |
|------------------------------------|-----------|
| `web.http.credentials.maxbodysize` | `5242880` |
| `web.http.issuance.maxbodysize`    | `5242880` |

If the `Content-Length` header exceeds the limit, the request is rejected before its body is read. Bodies without a
`Content-Length` header (chunked transfer encoding) are rejected as soon as more than the maximum number of bytes has
been read.

> Note to client implementors: when a request is rejected based on its `Content-Length` header, IdentityHub responds and
> closes the connection without reading the body. Clients that send the whole body before reading the response may then
> observe a connection reset instead of the `413` response. To reliably receive the `413`, clients should send the
> `Expect: 100-continue` header with large requests, so that the server can reject them before the body is transmitted.
> Some HTTP clients (e.g. curl) do this automatically for large bodies, others (e.g. Apache HttpClient) do not.

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