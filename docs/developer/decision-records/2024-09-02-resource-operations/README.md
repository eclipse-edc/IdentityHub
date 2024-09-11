# Identity Hub Resource Operations

This decision record defines the behavior of Identity Hub operations related to resources.

## Participant Context Operations

### 1. Creating a participant context: *createParticipantContext*

`ParticipantContextEventCoordinator` must listen to `ParticipantContextCreated` events and perform the following steps:

- A transaction is opened.
- An API key is generated.
- A DID document is created and added to storage.
- A default key pair is created and added to storage. Since the DID document resource is not in the `PUBLISHED` state,
  this action does not result in a new publication.
- If the participant context is active, the DID document is explicitly published.
- The transaction commits on success, or a rollback is performed.

### 2. Deleting a participant context: *deleteParticipantContext*

Deleting a participant context must be performed in the following sequence:

- A transaction is opened.
- The DID document is unpublished if the resource is in the `PUBLISHED` state.
- The DID document resource is removed from storage.
- All associated key pair resources are removed from storage except for private keys.
- The participant context is removed from storage.
- The transaction commits on success, or a rollback is performed.
- All private keys associated with the context are removed after the transaction is committed since Vaults are not
  transactional resources.

If destroying private keys fails, manual intervention will be required to clean them up. Note that the IH will be in a
consistent state.

### 3. Updating a participant context: *updateParticipant*

#### Activate

A participant context cannot be transitioned to the `ACTIVATED` state without a default key pair.

Transitioning a participant context to the `ACTIVATED` state must be performed in the following sequence:

- A transaction is opened.
- The context is updated in storage.
- The DID document is published.
- The transaction commits on success, or a rollback is performed.

#### Deactivate

Transitioning a participant context to the `DEACTIVATED` state must be performed in the following sequence:

- A transaction is opened.
- The context is updated in storage.
- The DID document is unpublished.
- The transaction commits on success, or a rollback is performed.

There is a `force` option that will commit the transaction if the DID document unpublish operation is not successful.

## DID Document Operations

### 1. DID Publishing

This operation can only be performed if the participant context is in the `ACTIVATED` state.

### 2. DID Unpublishing

This operation can only be performed if the participant context is being deactivated.

## Key Pair Operations

## 1. Activating key pairs: *activate*

This operation can be performed for the `CREATED` and `ACTIVATED` participant context states.

Activating a key pair and publishing associated changes to a DID document must be performed in the following sequence:

- A transaction is opened.
- key key pair resource's state is set to ACTIVATED
- The new (now active) key pair is added to the did document resource in its storage.
- If the DID document resource is in the `PUBLISHED` state, the DID document is published with all verification methods
  for public keys in the `ACTIVATED` state. Note that the DID document resource cannot be in the `PUBLISHED` state if
  the participant context is not `ACTIVATED`.
- The transaction commits on success, or a rollback is performed.

If publishing fails, the key pair is not committed to storage.

If the transaction commit fails, the DID document must be manually repaired. This can be done by republishing the DID
document.

It must not be possible to activate a key without publishing it.

## 2. Adding key pairs: *addKeyPair()*

This operation can be performed for the `CREATED` and `ACTIVATED` participant context states.

the following sequence takes place:

- open transaction
- add key pair resource to the storage
- if activation is false, the key pair is committed to storage.
- if activation is true, see activating key pair above.

## 3. Key rotation: *rotateKeyPair()*

The following operations must be performed:

- A transaction is opened.
- The new key pair is added to storage.
- If the DID document resource is in the `PUBLISHED` state, the DID document is published with a verification method for
  the new public key.
- The transaction commits on success, or a rollback is performed.
- The old private key is destroyed (note, not the old public key) after the transaction is committed since Vaults are
  not transactional resources.

If publishing fails, the new key pair is not committed to storage.

If the transaction commit fails, the DID document must be manually repaired. Note that IH will continue to function with
the old key pair.

If destroying the old private key fails, manual intervention will be required to clean it up. Note that the IH will
function with the new key pair.

## Key revocation: *revokeKey*

The following operations must be performed:

- A transaction is opened.
- The key pair state is updated.
- If the DID document resource is in the `PUBLISHED` state, the DID document is published with a verification method for
  the rotated public key removed.
- The transaction commits on success, or a rollback is performed.
