/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.keypair;

import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface KeyPairService {

    /**
     * Adds a new key pair to a participant and optionally makes it the default key pair. (Database) IDs are assigned automatically, by default using a GUID.
     *
     * @param participantId The participant ID to which the new key pair is supposed to belong.
     * @param keyDescriptor Contains either the new key in serialized form, or instructions how to generate it.
     * @param makeDefault   Whether this new key is supposed to be the default key for the participant.
     * @return a failure if the new key could not get created, success otherwise
     */
    ServiceResult<Void> addKeyPair(String participantId, KeyDescriptor keyDescriptor, boolean makeDefault);

    /**
     * Phases out an old key and creates a new one. The old key pair's private key gets deleted from the vault, so it cannot be used
     * to sign/encrypt anymore, but the public key stays in the DID document.
     * <ul>
     * <li>Add the new key to the DID document.</li>
     * <li>If the old key pair was the default pair, then this new key pair will become the default as well.</li>
     * <li>the new key pair will become active immediately</li>
     * </ul>
     *
     * @param oldId      the (database) ID of the key that is supposed to be rotated out.
     * @param newKeySpec the new key (or instructions how to generate it).
     * @param duration   Specifies the time (in millis) how long the old key should stay in rotation before getting expunged.
     * @return success if rotated, a failure indicated the problem otherwise.
     */
    ServiceResult<Void> rotateKeyPair(String oldId, KeyDescriptor newKeySpec, long duration);

    /**
     * Immediately deactivates a key pair and bars it from further use by deleting the private key from the {@link Vault} and removing the public key from
     * the DID document.
     * <ul>
     * <li>If a new key is specified, it'll get added to the DID document.</li>
     * <li>If the old key pair was the default pair, then this new key pair will become the default as well.</li>
     * <li>If no new key pair is specified, there may not be a default key pair after the rotation.</li>
     * <li>If the revoked key is the last active one, and no new key pair is specified, no error is returned, but the participant may be without active keys.</li>
     * </ul>
     *
     * @param id The database ID of the key to be revoked.
     * @return success if rotated, a failure indicated the problem otherwise.
     */
    ServiceResult<Void> revokeKey(String id, @Nullable KeyDescriptor newKeySpec);

    ServiceResult<Collection<KeyPairResource>> query(QuerySpec querySpec);

    /**
     * Sets a key pair to the {@link KeyPairState#ACTIVATED} state.
     *
     * @param keyPairResourceId The ID of the {@link KeyPairResource}
     * @return return a failure if the key pair resource is not in either {@link KeyPairState#CREATED} or {@link KeyPairState#ACTIVATED}
     */
    ServiceResult<Void> activate(String keyPairResourceId);
}
