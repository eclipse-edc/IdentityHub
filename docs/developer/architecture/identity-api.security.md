# On IdentityHub Identity API security

## 1. Definition of terms

- _Service principal_ (also referred as _principal_): the identifier for the entity that owns a resource. In
  IdentityHub, this is the ID of
  the `ParticipantContext`. Note that this is **not** a user!
- _User_: a physical entity that may be able to perform different operations on a resource belonging to a service
  principal. While a participant (context) would be analogous to a company or an organization, a user would be one
  single individual within that company / participant. **Individual users don't exist as first-level concept in
  IdentityHub!**
- _Participant context_: this is the unit of management, that owns all resources. Its identifier must be equal to
  the `participantContextId` that is defined
  in [DSP](https://github.com/International-Data-Spaces-Association/ids-specification). For the purposes of Identity
  API operations, IdentityHub assumes the ID of the `ParticipantContext` to be equal to the ID of
  the `ServicePrincipal`.
- Identity API: collective term for all endpoints that serve the purpose of managing participant contexts and their
  resources. Also referred to as: mgmt api
- API Key: a secret string that is used to authenticate/authorize a request and that is typically sent in the HTTP
  header. Also referred to as: API token, API secret, credential
- Scope: a permission granted to a service principal, of the form `<api>[:<resource>]:<action>`, e.g.
  `identity-api:keypairs:write`. Also referred to as: permission
- Super-user: service principal with the admin scope of an API (`identity-api:admin`, `issuer-admin-api:admin`), which
  grants elevated access permissions. Also referred to as: admin, root user

## 2. Requirements

### 2.1 Authentication of ServicePrincipals

When Identity API requests are received by the web server, it (or a related function) must be able to derive
the `ServicePrincipal` from the request context. The `ServicePrincipal` is the internal representation of a user. In
IdentityHub, users are called "participant contexts". In other words, IdentityHub must be able to determine which
participant sent the request.
For that, the Identity API employs methods that are widely known, such as Basic Auth or API keys.

Authentication (=user identification) should happen before the request is matched onto a controller method, so that the
handling controller method can inject the `ServicePrincipal` using standard JAX-RS features:

```java

@POST
@Path("/foo/bar")
public void someEndpoint(@Context SecurityContext securityContext) {
    var principal = securityContext.getUserPrincipal();
    // do something with the SP
}
```

Note that if the `ServicePrincipal` cannot be determined, the request **must** be rejected with a HTTP 401 or HTTP 404
error code _before_ it reaches the controller. `@Context` is a standard JAX-RS annotation.

### 2.2 Authorization of requests

Identifying the `ServicePrincipal` alone is not enough, because the webserver must also be able to determine whether the
service principal is allowed to access a particular resource. In practice, this is to guard against participants reading
or - even worse - modifying resources that they don't own. Since this cannot be done reliably during request ingress, a
service is needed to perform the resource lookup.

### 2.3 Elevated access

Some operations in the Identity API _require_ elevated access rights, for example modifying participant contexts, or
listing resources across multiple participant contexts. The elevated access is tied to
the [admin scope](#51-scopes) of the API. The super-user has that scope.

In addition, the super-user is able to perform every operation "on behalf of" a normal user. That means even if the
super-user's `ServicePrincipal` does not technically own a resource, permissions to read and modify are still granted.

## 3. Authentication

### 3.1 Authenticating a request

To access the Identity API, every request must contain the `x-api-key` header, which contains the API key of the
participant. This is a string that contains the service principal's ID (=`spId`) followed by a randomly generated
character sequence. Both parts are base64-encoded:

```
base64(spId)+"."+base64(randomString)
```

> Note: the API key is a secret, do not divulge or share it. [Regenerating it](#33-regenerating-the-api-key)
> periodically and on suspicion of leakage is strongly recommended.

During request ingress, the API key is used to lookup the service principal by performing the following steps:

- verify that `spId` has the correct structure, otherwise abort with HTTP 401
- decode first part and interpret as `spId`
- perform database lookup to obtain the `ServicePrincipal`
- if no result, abort with HTTP 401
- check that the `ServicePrincipal`'s credential matches the API key
- if mismatched, abort with HTTP 401

After the service principal is authenticated, it is attached to the request's `SecurityContext` to allow controllers to
inject it for further processing.

### 3.2 Obtaining an API key

API keys are generated automatically when a new `ParticipantContext` is created. This API operation requires elevated
access and can thus only be done by the super-user, and returns the new API key in the HTTP response.

IdentityHub's Identity API does not provide a feature for participant self-registration, as it is not an
end-user-facing API. This is intentional. New participant contexts must be created by the super-user.

> The initial API key must be transmitted to the participant by the super-user in a secure out-of-band channel.
> Appropriate security measures to protect the API key are advised.

### 3.3 Regenerating the API key

Once the participant has received the initial API key, it is highly recommended that it is immediately regenerated using
the Identity API. Note that the initial API Key is required for that operation:

```shell
curl -X POST -H "x-api-key: <initial-api-key>" "http://your-identityhub.com/.../v1/participants/<participant-id>/token"
```

The new API key is returned in the response body as plain text.

### 3.4 Regenerating the STS client secret

If the embedded Secure Token Service (STS) is used, a client secret for the participant's STS account is generated when
the `ParticipantContext` is created and returned once. Like the API key, it can be regenerated by the participant or the
super-user:

```shell
curl -X POST -H "x-api-key: <api-key>" "http://your-identityhub.com/.../v1/participants/<participant-id>/sts/secret"
```

The new client secret is returned in the response body as plain text, and the previous one is no longer valid. It is
stored under the same vault alias as before.

## 4. Authorization

### 4.1 Explicit resource authorization

In order to grant or deny access to a particular resource, we need to determine, whether the requesting service
principal owns that resource.

Unfortunately it is not possible to derive that information from request context alone. For one, we'd have to employ a
certain amount of guess work to determine which part of the request path is the resource ID, take this path for example:

```
/v1/participants/<participantContextId>/keypairs/<keypairId>
```

While it would be _theoretically_ possible to employ a string parsing method, where we interpret the third path
element (the one that follows `/participants/`) as service principal ID, and the fifth one as resource ID, this would be
a very brittle solution, because it would require a rigid path structure and it would break as soon as paths are
changed. In addition, it would suffer from the fact that the _type_ of resource cannot be determined reliably.

Authorization is performed _after_ a request reaches the controller, and it is implemented as an explicit method call to
the `AuthorizationService` in the controller method, for example:

```java

@GET
@Path("/{keyPairId}")
@Override
public KeyPairResource findById(@PathParam("keyPairId") String id, @Context SecurityContext securityContext) {

    authorizationService.authorize(securityContext, id, KeyPairResource.class)
            .orElseThrow(exceptionMapper(KeyPairResource.class, id));

    //  DB operation
}
```

Here, the `AuthorizationService` is consulted to determine whether `participantContextId` is permitted to access
a `KeyPairResource` with `id`.

> There is one exception to this: a service principal with the [admin scope](#51-scopes) of the API is authorized to
> access the resources of every participant context.

### 4.2 The `AuthorizationService` and resource lookup functions

As mentioned before, the `AuthorizationService` is responsible to establish the link between service principal and
resource. It is a normal service which can be injected into controller classes.

Every Identity API module then contributes a _lookup function_ and the resource type it handles, which
the `AuthorizationService` maintains and to which it dispatches based on resource class. For example the DID document
management Identity API module:

```java

@Extension(value = NAME)
public class DidManagementApiExtension implements ServiceExtension {
    @Inject
    private DidDocumentService didDocumentService;

    // other fields, constants, injects

    @Override
    public void initialize(ServiceExtensionContext context) {
        authorizationService.addLookupFunction(DidResource.class, (owner, id) -> didDocumentService.findById(id));
        // other initialization
    }
}
```

The job of the lookup function is to retrieves a resource from the database.

It is invoked by the `AuthorizationService`, which then checks whether the returned resource is owned by
the `ServicePrincipal` who made the request (see [3. Authentication](#3-authentication)):

```java
public class AuthorizationServiceImpl implements AuthorizationService {
    private final Map<Class<?>, BiFunction<String, String, ParticipantResource>> resourceLookupFunctions = new HashMap<>();

    @Override
    public ServiceResult<Void> authorize(SecurityContext securityContext, String resourceOwnerId, String resourceId, Class<? extends ParticipantResource> resourceClass) {

        var function = resourceLookupFunctions.get(resourceClass);
        if (function == null) {
            return ServiceResult.unauthorized(/* error message */);
        }

        var resource = function.apply(resourceOwnerId, resourceId);
        if (resource == null) {
            return ServiceResult.notFound(/* error message */);
        }

        // a principal holding an admin scope is elevated and may access resources across participant contexts
        if (securityContext.getUserPrincipal() instanceof ParticipantPrincipal principal && isAdmin(principal.scope())) {
            return ServiceResult.success();
        }

        var securityPrincipalName = securityContext.getUserPrincipal().getName();
        if (!Objects.equals(securityPrincipalName, resourceOwnerId) || !Objects.equals(resource.getParticipantContextId(), resourceOwnerId)) {
            return ServiceResult.unauthorized(/* error message */);
        }

        return ServiceResult.success();
    }
}
```

## 5. Scope-based access control

So far, we have only granted access based on whether a service principal _owns_ a resource or not.
In other words, if _only_ the resource owner is granted access, not even the super-user can modify a participant's
key-pair resources.

This is not sufficient, because the super-user should have access to all resources, e.g. when API keys are lost or
compromised, or participants require administrative assistance. In addition, a service principal should only be able to
perform the operations it needs, e.g. read its credentials, but not rotate its keys. Both are expressed with _scopes_.

### 5.1 Scopes

A scope has the form `<api>[:<resource>]:<action>`, where `<action>` is `read`, `write` or `admin`:

- the Identity API uses the namespace `identity-api`, with the resources `participants`, `keypairs`, `dids` and
  `credentials`, e.g. `identity-api:keypairs:write`;
- the IssuerService administration API uses the namespace `issuer-admin-api`, with the resources `attestations`,
  `credentialdefinitions`, `credentials`, `holders` and `issuanceprocesses`.

A stronger action includes the weaker ones (`admin` ⊇ `write` ⊇ `read`), and a scope without a resource applies to all
resources of the API: `identity-api:write` grants every `identity-api:<resource>:write` and `:read` scope. The constants
are defined in `IdentityApiScopes` and `IssuerAdminApiScopes`.

The admin scope (`identity-api:admin`, `issuer-admin-api:admin`) elevates the service principal: it may access the
resources of every participant context, and call the endpoints reserved to the super-user, e.g. creating participant
contexts, activating and deactivating them, or setting their scopes. A service principal with an admin scope is the
super-user; there may be several. Each API only honours its own admin scope: `issuer-admin-api:admin` doesn't elevate
a principal on the Identity API (`ApiScopeFilter` narrows the scopes of a principal to the namespace of the API).

Every controller method declares the scope it requires with `@RequiredScope`, which is checked before the request
reaches the controller. A method without `@RequiredScope` can't be called at all: its access is denied, and a warning is
logged at startup.

```java

@POST
@RequiredScope(IdentityApiScopes.ADMIN)
public CreateParticipantContextResponse createParticipant(ParticipantManifest manifest) {
    // impl
}
```

Where the scopes of a service principal come from depends on the authentication method:

- with API keys, they are stored with the participant context: set in the `scopes` of the `ParticipantManifest` when it
  is created, or with `PUT /v1/participants/{participantContextId}/scopes` by the super-user. A participant context
  created without scopes gets `identity-api:read`, `identity-api:write`, `issuer-admin-api:read` and
  `issuer-admin-api:write`;
- with OAuth2 tokens, they are the `scope` claim of the token, issued by the authorization server.

Out-of-the-box, IdentityHub does **not have** any users/participant contexts/principals configured: the operator creates
the first participant context with an admin scope, e.g. with an OAuth2 token issued for the super-user.

### 5.2 Extensibility

The scope checks and the resource ownership checks can be customized by providing another `AuthorizationService`, e.g.
to grant a principal access to the key pairs of all participant contexts based on a custom scope:

```java
public class KeyPairAdminAuthorizationService implements AuthorizationService {
    @Override
    public ServiceResult<Void> authorize(SecurityContext securityContext, String resourceOwnerId, String resourceId, Class<? extends ParticipantResource> resourceClass) {
        if (KeyPairResource.class.equals(resourceClass) && hasScope(securityContext, "identity-api:keypairs:admin")) {
            return ServiceResult.success();
        }
        return delegate.authorize(securityContext, resourceOwnerId, resourceId, resourceClass);
    }
}
```

> This is just an example to illustrate the concept.

## 6. Modularity and pluggability

IdentityHub's authentication and authorization frameworks can be individually customized, swapped out or dropped
completely.

While technically possible, swapping out the authentication module is **not** recommended at this point, because there
is essential functionality implemented there such as resolving the `ServicePrincipal` from the database or realizing the
elevated access restriction on some controller methods.

However, swapping out the authorization module is possible, and necessary if the scope-based access control is
supposed to be customized.

## 7. General security considerations

- the Identity API is **not** intended for public consumption and thus should **never** be exposed to the internet
  directly.
- if management operations must be accessbile over the internet, additional security measures such as API gateways,
  firewalls, additional authorization frontends, etc. must be in place. The specifics of that are beyond the scope of an
  open-source project.
- API keys and STS client secrets are secrets that should **never** be shared. If there is a chance that they leaked, it
  is recommended to re-generate them.
