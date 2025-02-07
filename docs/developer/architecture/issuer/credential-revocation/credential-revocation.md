# Credential status information in the IssuerService

Most verifiable credentials expire naturally, as setting an expiry date is a highly recommended practice.
However, there could be cases where a credential is put on hold, withdrawn or revoked prematurely, for instance when
a participant offboards from a dataspace.

Verifiable credentials may contain a `credentialStatus` object (
c.f. [here](https://www.w3.org/TR/vc-data-model/#status)), which contains information about a remote location, typically
also a verifiable credential, that contains status information about the holder credential. This remote credential
contains an encoded and compressed bitstring with a label, e.g. "revocation" and if the bit at the given index is "1",
the label applies. For example, if the label is `"revocation"` and the bit is "1", then the credential is revoked.
Common status labels are "suspension" and "revocation", sometimes also "refresh".

This remote credential is typically referred to as the "status list" or "status list credential" as it provides
information about a given status (= the label) of a certain holder credential. Similarly, all credentials that are used
by holders to prove a claim are referred to as "holder credentials".

In order for credential status lists to work we must make a few general assumptions:

- IssuerService can only revoke holder credentials it has issued (can't revoke other issuers' credentials)
- IssuerService only issues credentials with a `credentialStatus` object
- Each status purpose has its own status list credential, and correspondingly, there is one corresponding
  `credentialStatus` object in the holder credential. So if an IssuerService supports revocation and suspension, it'll
  provide two status list credentials
- setting the status bit is an idempotent operation: setting an already set status bit is a NOOP and can be
  short-circuited
- status list credentials are always stored in signed JWT form. Other content types may be supported,
  cf. [here](#using-the-publicly-available-status-list-credential).

## Creating credentials with status information

Whenever the IssuerService creates (_issues_) a credential, it must update the status list credential accordingly. If
the status list credential is full, i.e. all the bits are occupied, the IssuerService must create and publish a new
status list credential.

[//]: # (todo: add documentation about status list credential sizing and partitioning logic)

The resulting credential URL and status list index must be added to the `credentialStatus` object of the pending holder
credential:

```json
{
  "id": "https://your.issuer.com/credentials/status/revocation/4#23452",
  "type": "BitstringStatusListEntry",
  "statusPurpose": "revocation",
  "statusListIndex": "00001",
  "statusListCredential": "https://your.issuer.com/credentials/status/revocation/4"
}
```

## Revoking a holder credential

Requests to revoke a holder credential are sent via the IssuerService's REST API:

```http request
POST /v1alpha/credentials/{credentialId}/revoke
```

This request is processed by
the [IssuerCredentialsAdminApiController](../../../../../extensions/api/issuer-admin-api/credentials-api/src/main/java/org/eclipse/edc/issuerservice/api/admin/credentials/v1/unstable/IssuerCredentialsAdminApiController.java)
and delegated to the `CredentialServiceImpl`.

The `credentialId` is the only information that is required to perform the necessary actions. The
`CredentialServiceImpl` performs the following steps:

#### 1. obtain holder credential

fetch the holder's credential information from the database. Note that these objects _never_ contain any keys or the
signed credential, only metadata.

#### 2. obtain status info for holder credential

the ID of the relevant status list credential can be obtained by inspecting the holder credential's `credentialStatus`
object:

```json
{
  "id": "https://your.issuer.com/credentials/status/revocation/4#23452",
  "type": "BitstringStatusListEntry",
  "statusPurpose": "revocation",
  "statusListIndex": "00001",
  "statusListCredential": "https://your.issuer.com/credentials/status/revocation/4"
}
```

thus, the ID of the status list credential is `https://your.issuer.com/credentials/status/revocation/4`. The status list
credential is a normal verifiable credential, that is stored in the IssuerService's internal database (using the
`VerifiableCredentialResource` structure), so it can fetched from the database in the same manner, using the
`CredentialStore`.

_This is where things start to differ for various status list implementations._

The shape of the status list credential is slightly different for various implementations, so this is where we delegate
based on the `credentialStatus.type` field. Using a registry pattern, a `StatusListInfo` object is created for each
individual status list type:

```java
var revocationInfo = ofNullable(statusListInfoFactoryRegistry.getStatusListCredential(status.type()))
        .map(factory -> factory.create(status));
```

this method loads the status list credential from the database and parses its `credentialSubject`. The
`StatusListInfoFactory` creates a `StatusListInfo`, that acts as wrapper for the concrete status list information,
together with the holder's `VerifiableCredentialResource`.

For example, the `BitstringStatusList` implementation looks like this:

```java
public class BitstringStatusListFactory implements StatusListInfoFactory {
    @Override
    public ServiceResult<StatusListInfo> create(CredentialStatus credentialStatus) {


        var statusListCredentialId = credentialStatus.getProperty(BITSTRING_STATUS_LIST_PREFIX, BITSTRING_STATUS_LIST_CREDENTIAL_LITERAL);
        var index = credentialStatus.getProperty(BITSTRING_STATUS_LIST_PREFIX, BITSTRING_STATUS_LIST_INDEX_LITERAL);

        if (statusListCredentialId == null || index == null) {
            // handle error
        }

        // fetch status list credential from DB
        var result = credentialStore.findById(statusListCredentialId.toString());
        return result.succeeded()
                ? success(new BitstringStatusInfo(index, result.getContent()))
                : ServiceResult.fromFailure(result);
    }
}
```

The `StatusListInfo` object's job is to convert the generic data model of verifiable credentials specifically for
the given data model, here, `BitstringStatusList`.

_Note: `StatusListInfo` implementations should operate on the object model, **not** on the encoded credential (
`VerifiableCredentialContainer#rawVc`). The `StatusListInfo` object encapsulates the specifics of each status list
variant._

#### 3. check and set new status

The `StatusListInfo` object exposes methods to get and set the status bit. The encoding/compressing algorithm may vary
for each type. This process is agnostic of the status purpose: it does not matter whether the status bit for "
revocation" or "suspension" is set. So the same implementation can be used for `"revocation"`, `"suspension"`, etc.

After the `StatusListInfo` object is created, the `CredentialServiceImpl` sets the status bit:

```javascript
var setStatusResult = statusListInfo.setStatus(true);
if (setStatusResult.failed()) {
    return unexpected(setStatusResult.getFailureDetail());
}
```

Setting the status simply updates the object model, it does **not** encode and sign the credential as JWT. This happens
in the next step.

#### 4. re-encode status list credential

After the status bit is updated, the status list credential object is encoded as JWT and signed with the given private
key.
> this private key is configured using the `edc.issuer.statuslist.signing.key.alias` config property

Technically speaking, this updates the `VerifiableCredentialResource#verifiableCredential#rawVc` value with the signed
and
serialized JWT.

#### 5. update status list credential and holder credential

Both the status list credential and the holder credential are updated in the database.
With this, the revocation process is complete.

## Querying credential status

This is the process of obtaining status information for a holder credential.

### Option 1: Using the REST API

This is the easiest approach, as only the `ID` of the holder credential is needed, but access to the IssuerService's
Admin API is required. In addition, this API is only available for credentials that were originally issued by this
IssuerService.

Execute the following REST request against the IssuerService's Admin API:

```http request
GET /v1alpha/credentials//{credentialId}/status
```

the response will be `HTTP 200` if the status information has been obtained. An empty response body indicates that the
status is not set (e.g. "not revoked"). If the status is set, the status label is returned in the response body, e.g.
`"revocation"`.

### Option 2: Resolving the status list credential

This is the generic approach that is available to everyone. Specifics depend on the implementation of the status list,
but the general process is: resolve status list credential -> decode bitstring -> obtain bit at index -> interpret
status purpose.

The IssuerService stores status list credentials in signed and encoded form (JWT), and this is also the format in which
they are served by default.

Serving status list credentials should be done in such a way that the `Accept` header of the request is interpreted and
used to shape the response, using `application/vc+jwt` as default.

The IssuerService's status list credential endpoint will respond with `HTTP 415` if the `Accept` header calls for a
media type that cannot be served.

At this time, only the following media type headers are available:

- `Accept: application/vc+jwt`: serves the status list credential as signed JWT. The concrete format (VC DataModel 1.1
  or 2.0)
  is determined by the status list type.
- `Accept: application/vc`: the encoding, proof type and data model is left up to the server
- `Accept: application/json`: serves the status list credential in plain JSON form without proof. NOT RECOMMENDED!
- none: defaults to `Accept: application/vc+jwt`
