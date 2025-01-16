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
- Super-user: service principal with pre-defined roles that grant elevated access permissions. Also referred to as:
  admin, root user

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
a [built-in role](#51-built-in-roles). The super-user has that role.

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

    authorizationService.isAuthorized(securityContext, id, KeyPairResource.class)
            .orElseThrow(exceptionMapper(KeyPairResource.class, id));

    //  DB operation
}
```

Here, the `AuthorizationService` is consulted to determine whether `participantContextId` is permitted to access
a `KeyPairResource` with `id`.

> There is one exception to this, which is authorizing [built-in roles](#51-built-in-roles).

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
        authorizationService.addLoookupFunction(DidResource.class, did -> didDocumentService.findById(did));
        // other initialization
    }
}
```

The job of the lookup function is to retrieves a resource from the database.

It is invoked by the `AuthorizationService`, which then checks whether the returned resource is owned by
the `ServicePrincipal` who made the request (see [3. Authentication](#3-authentication)):

```java
public class AuthorizationServiceImpl implements AuthorizationService {
    private final Map<Class<?>, Function<String, ParticipantResource>> resourceLookupFunctions = new HashMap<>();

    @Override
    public ServiceResult<Void> isAuthorized(Principal principal, String resourceId, Class<?> resourceClass) {

        var function = resourceLookupFunctions.get(resourceClass);
        if (function == null) {
            return ServiceResult.unauthorized(/* error message */);
        }

        var isAuthorized = ofNullable(function.apply(resourceId))
                .map(pr -> Objects.equals(pr.getParticipantId(), principal.getName()))
                .orElse(false);

        return isAuthorized ? ServiceResult.success() : ServiceResult.unauthorized(/* error message */);

    }
}
```

## 5. Role-based access control (RBAC)

So far, we have only granted access based on whether a service principal _owns_ a resource or not.
In other words, if _only_ the resource owner is granted access, not even the super-user can modify a participant's
key-pair resources.

This is not sufficient, because the super-user should have access to all resources, e.g. when API keys are lost or
compromised, or participants require administrative assistance. Thus, as per default behaviour, the built-in super-user
role is granted access to all resources of all participant contexts.

From an operations perspective it may even become necessary to have a more elaborate permissions concept in place. For
example, there may be a "security-admin" role, that manages key pair resources and thus has to have access to _all_ key
pair resources or all participants, but is denied access to everything else.

### 5.1 Built-in roles

Out-of-the-box, IdentityHub comes with only one single built-in role called `"admin"`. This role is comparable to
the `root` user on a Unix system. By default, there is one service principal that has the `"admin"` role, which is the
super-user. While the service principal's name is arbitrary, the role it assumes is `"admin"`. Other service
principals can assume the `"admin"` role.

> Note: the name `"admin"` is available via a constant `ServicePrincipal.ROLE_ADMIN`

All other service principals (i.e. `ParticipantContexts`) have no roles assigned to them when they are created.

Built-in roles cannot be changed, and they cannot be configured, they are essentially hard-wired into the code base.
Some endpoints are accessible _only_ to principals with the `"admin"` role. The authorization decision is made based on
the `jakarta.annotation.security.RolesAllowed` annotation on the controller method, even before the request hits the
controller, therefor this value must be compile-time constant.

For example creating `ParticipantContexts`:

```java

@POST
@RolesAllowed(ServicePrincipal.ROLE_ADMIN)
public String createParticipant(ParticipantManifest manifest) {
    // impl
}
```

> Using static role matching for the super-user was a conscientious architectural decision, as the business value of
> configuring the name of the `"admin"` role was deemed minimal.

Out-of-the-box, IdentityHub does **not have** any users/participant contexts/principals configured.

### 5.2 Extensibility

Out-of-the box, IdentityHub comes with a pretty basic role concept: there is the `"admin"`, and there is everyone else.

Depending on the deployment scenario and the complexity of the installation, a more elaborate RBAC concept with more
permission levels may be required. Thus, the RBAC system used by EDC is extensible in two aspects:

1. role concept
2. access permissions based on roles

#### 5.2.1 Customize roles via Identity API

Roles are just labels that a `ParticipantContext` has attached to them, and there is no need for a complicated rule
engine. The super-user can assign arbitrary roles to participants.

New `ParticipantContexts` don't have any roles assigned to them when they are created.

#### 5.2.2 Customize access permissions

Developers can customize the set of roles that are available in an IdentityHub, and they can customize the permissions
associated with each role. There is no rule engine or complicated permissions model, but rather the customization is
achieved by simply extending the `AuthorizationService`.
There are several ways to implement RBAC:

- based on the resource type. Here, the principal with the `"security-admin"` role is granted access to objects of type
  `KeyPairResource`:
  ```java
  public class ResourceTypeBasedAuthService implements AuthorizationService {
    @Override
    public ServiceResult<Void> isAuthorized(SecurityContext securityContext, String resourceId, Class<?> resourceClass) {
        if(KeyPairResource.class.equals(resourceClass) && securityContext.isUserInRole("security-admin")){
            return ServiceResult.success();
        }        
        return ServiceResult.unauthorized(/*error message*/)
    }
  }
  ```

- based on the individual resource ("by-ID"). Here, the principal with the `"security-admin"` role is granted access to
  objects with white-listed IDs:

  ```java
    public class ResourceInstanceBasedAuthService implements AuthorizationService {
    @Override
    public ServiceResult<Void> isAuthorized(SecurityContext securityContext, String resourceId, Class<?> resourceClass) {
        if(ALLOWED_RESOURCE_IDS.contains(resourceId) && securityContext.isUserInRole("security-admin")){
            return ServiceResult.success();
        }        
        return ServiceResult.unauthorized(/*error message*/)
    }
  }
  ```

  > These are just examples to illustrate the concept. Do not use constants to specify allowed resources.

## 6. Modularity and pluggability

IdentityHub's authentication and authorization frameworks can be individually customized, swapped out or dropped
completely.

While technically possible, swapping out the authentication module is **not** recommended at this point, because there
is essential functionality implemented there such as resolving the `ServicePrincipal` from the database or realizing the
elevated access restriction on some controller methods.

However, swapping out the authorization module is possible, and necessary if RBAC is supposed to be customized.

## 7. General security considerations

- the Identity API is **not** intended for public consumption and thus should **never** be exposed to the internet
  directly.
- if management operations must be accessbile over the internet, additional security measures such as API gateways,
  firewalls, additional authorization frontends, etc. must be in place. The specifics of that are beyond the scope of an
  open-source project.
- API keys are secrets that should **never** be shared. If there is a chance that they leaked, it is recommended to
  re-generate them.
