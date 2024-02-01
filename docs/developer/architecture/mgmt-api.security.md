# On IdentityHub Management API security

## 1. Definition of terms
- _Service principal_: the identifier for the entity that owns a resource. In IdentityHub, this is the ID of the `ParticipantContext`. Also referred to as: principal
- _User_: a physical entity that may be able to perform different operations on a resource belonging to a service principal. ** **Individual users don't exist as first-level concept in IdentityHub!**
- _Participant context_: this is the unit of management, that owns all resources. Its identifier must be equal to the `participantId` that is defined in [DSP](https://github.com/International-Data-Spaces-Association/ids-specification). IdentityHub assumes the `ParticipantContext.ID` to be the `ServicePrincipal`.
- Management API: collective term for all endpoints that serve the purpose of managing participants their resources. Also referred to as: mgmt api
- API Key: a secret string that is used to authenticate/authorize a request and is typically sent in the HTTP header. Also referred to as: API token, API secret, credential
- Super-user: service principal with elevated access permissions. Also referred to as: admin, root user

## 2. Requirements

### 2.1 Authentication of ServicePrincipals

When Management API requests are received by the web server, it (or a related function) must be able to derive the `ServicePrincipal` from the request context. In other words, it must be determined _who_ sent the request. 
For that, the Management API should employ methods that are widely known such as Basic Auth or API keys. 

Authentication (=user identification) should happen before the request is matched onto a controller method, so that the handling controller method can inject the ServicePrincipal using standard JAX-RS features:
```java
@POST
@Path("/foo/bar")
public void someEndpoint(@Context SecurityContext securityContext){
   var principal = securityContext.getUserPrincipal();
   // do something with the SP
}
```

Note that if the `ServicePrincipal` cannot be determined, the request **must** be rejected with a HTTP 401 or HTTP 404 error code _before_ it reaches the controller.
### 2.2 Authorization of requests

Identifying the `ServicePrincipal` alone is not enough, because the webserver must also be able to determine whether the SP is allowed to access a particular resource. In practice, this guards against participants reading or - even worse - modifying resources that don't belong to them. Since this cannot be done reliably during request ingress, a service is needed to perform the resource lookup.

### 2.3 Elevated access

Some operations in the Management API _require_ elevated access rights, for example modifying participant contexts, or listing resources across multiple participant contexts. The elevated access is tied to the "super-user", which is identified by a certain role property.

In addition, the super-user is able to perform every operation "on behalf of" a normal user. That means even if the super-user's `ServicePrincipal` does not technically own a resource, permission is still granted. 

## 3. Authentication

### 3.1 Authenticating a request

By default, every request must contain the `x-api-key` header, which contains the API key of the participant. This is an opaque string that encodes the service principal's ID (=`spId`) followed by a randomly generated character sequence. Both parts are base64-encoded: 
```
base64(spId)+"."+base64(randomString)
```

During request ingress, the API key is used to lookup the service principal by performing the following steps:
- decode first part and interpret as `spId`
- perform database lookup
- if no result, abort with HTTP 401
- check that the `ServicePrincipal`'s credential matches the API key
- if mismatch, abort with HTTP 401

After the service principal is authenticated, it is attached to the request's `SecurityContext` to allow controllers to inject it for further processing.

### 3.2 Obtaining an API key

API keys are generated automatically when a new `ParticipantContext` is created. This API operation requires elevated access and can thus only be done by the super-user, and returns the new API key in the HTTP response. 

> There is no self-registration! The initial API key must be transmitted to the participant by the super-user out-of-band.

### 3.3 Regenerating the API key

Once the participant has received the initial API key, it is highly recommended that it is immediately regenerated using the Management API:

```shell
curl -X POST -H "x-api-key: <initial-api-key>" "http://your-identityhub.com/.../v1/participants/<participant-id>/token"
```

The new API key is returned in the response body as plain text.

## 4. Authorization

### 4.1 The problem with authorization

Authorization must be performed _after_ a request reaches the controller, as an explicit method call in the controller method. 
Unfortunately it is not possible to derive the resource ID from the request context alone. For one, we'd have to employ a certain amount of guess work to determine which part of the request path is the resource ID, take this path for example:

```
/v1/participants/<participantId>/keypairs/<keypairId>
```

While it would _theoretically_ be possible to interpret the third path element (the one that follows `/participants/`) as service principal ID, and the fifth one as resource ID, this would be a very brittle solution, because it would require a rigid path structure or would involve a fair amount of guess work, and it would also suffer from the fact that the _type_ of resource cannot be determined.

### 4.2 The AuthorizationService and resource lookup functions

Thus, we introduced an `AuthorizationService`, which can be injected into controller classes.
Every management API module contributes a _lookup function_ and the resource type it handles, e.g. the DID document management API module:

```java
@Extension(value = NAME)
public class DidManagementApiExtension implements ServiceExtension {    
    // fields, constants, injects
    
    @Override  
    public void initialize(ServiceExtensionContext context) {  
        authorizationService.addLoookupFunction(DidResource.class, did -> didDocumentService.findById(did));  
      // other initialization
    } 
}
```

The lookup function is essentially a function that retrieves a resource from the database. 

The `AuthorizationService` invokes the lookup function, and checks whether the returned resource is owned by the `ServicePrincipal` who made the request (see [3. Authentication](#3-authentication)):

```java
public class AuthorizationServiceImpl implements AuthorizationService {  
    private final Map<Class<?>, Function<String, ParticipantResource>> authorizationCheckFunctions = new HashMap<>();  
  
    @Override  
    public ServiceResult<Void> isAuthorized(Principal principal, String resourceId, Class<?> resourceClass) {  
  
        var function = authorizationCheckFunctions.get(resourceClass);  
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
To be precise, _only_ the resource owner is granted access, for example, not even the super-user can modify a participant's key-pair resources.
From an operations perspective it may become necessary to have a more elaborate permissions concept in place. For example, there may be a "security-admin" role, that has access to _all_ key pair resources or all participants, but is denied access to everything else.

### 5.1 Built-in roles

Out-of-the-box IdentityHub comes with only one single built-in role called `"admin"`. This role is comparable to the `root` user on a Unix system. By default, there is one service principal that has the `"admin"` role, which is the super-user. 

> Note: the name `"admin"` is available via a constant `ServicePrincipal.ROLE_ADMIN`

All other service principals (ie. `ParticipantContexts`) have no roles assigned to them. 

Built-in roles cannot be changed and they cannot be configured. In addition, some endpoints are restricted to the super-user (i.e. the `"admin"` role). The authorization decision is made based on the `jakarta.annotation.security.RolesAllowed` annotation on the controller method, even before the request hits the controller, therefor this value must be compile-time constant.

For example creating `ParticipantContexts`:
```java
@POST
@RolesAllowed(ServicePrincipal.ROLE_ADMIN)
public String createParticipant(ParticipantManifest manifest) {
    // impl
}
```

### 5.2 Extensibility

Depending on the deployment scenario and the complexity of the installation, a more elaborate RBAC concept may be required. Thus, the RBAC system used by EDC is extensible in two aspects:
1. role concept
2. access permissions based on roles

#### 5.2.1 Customize roles via Management API

Since `ParticipantContexts` don't have any roles assigned to them, it is necessary to modify 

#### 5.2.2 Customize access permissions

Consequently, developers can customize the set of roles that are available in an IdentityHub, and they can customize the permissions associated with each role. 

Simply by extending the `AuthorizationService` developers have several possibilities to implement RBAC: 

- based on the resource type:
  ```java
  public class CustomAuthService implements AuthorizationService {
    @Override
    public ServiceResult<Void> isAuthorized(SecurityContext securityContext, String resourceId, Class<?> resourceClass) {
        if(KeyPairResource.class.equals(resourceClass) && securityContext.isUserInRole("security-admin")){
            return ServiceResult.success();
        }        
        return ServiceResult.unauthorized(/*error message*/)
    }
  }
  ```

- based on the individual resource ("by-ID"):

  ```java
    public class CustomAuthService implements AuthorizationService {
    @Override
    public ServiceResult<Void> isAuthorized(SecurityContext securityContext, String resourceId, Class<?> resourceClass) {
        if(ALLOWED_RESOURCE_IDS.contains(resourceId) && securityContext.isUserInRole("security-admin")){
            return ServiceResult.success();
        }        
        return ServiceResult.unauthorized(/*error message*/)
    }
  }
  ```

  > Note that these are just examples to illustrate the concept.


## 6. Modularity and pluggability

IdentityHub's authentication and authorization frameworks can be individually customized, swapped out or dropped completely. 

While technically possible, swapping out the authentication module is **not** recommended at this point, because there is essential functionality implemented there such as resolving the `ServicePrincipal` from the database or realizing the elevated access restriction on some controller methods. 

However, swapping out the authorization module is possible, and necessary if RBAC is supposed to be customized. 

## 7. General security considerations
- the Management API is **not** intended for public consumption and thus should only be accessible from within internal networks. 
- if the Management API is exposed to the internet, additional security measures should be taken. The specifics of that are beyond the scope of an open-source project.
- API keys are secrets that should **never** be shared. If there is a chance that they leaked, it is recommended to re-generate them.
