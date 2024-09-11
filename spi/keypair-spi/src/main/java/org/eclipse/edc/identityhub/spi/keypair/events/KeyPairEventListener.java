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

package org.eclipse.edc.identityhub.spi.keypair.events;

import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.spi.observe.Observable;

/**
 * Interface implemented by listeners registered to observe key pair resource changes via {@link Observable#registerListener}.
 * The listener must be called after the state changes are persisted.
 */
public interface KeyPairEventListener {

    /**
     * A {@link KeyPairResource} was added to a particular {@link ParticipantContext}. This could happen either
     * by simply adding a keypair, or after a keypair was revoked, and a successor was specified.
     *
     * @param keypair The new (added) key pair
     * @param type    Verification type specifying the cryptographic context in which the public key is used, e.g. JsonWebKey2020...
     */
    default void added(KeyPairResource keypair, String type) {

    }

    /**
     * A {@link KeyPairResource} was rotated (=phased out). If the rotation was done with a successor keypair, this would be communicated using the {@link KeyPairEventListener#added(KeyPairResource, String)}
     * callback.
     *
     * @param keyPair the old (outgoing) {@link KeyPairResource}
     */
    default void rotated(KeyPairResource keyPair) {

    }

    /**
     * A {@link KeyPairResource} was revoked (=deleted). If the revocation was done with a successor keypair, this would be communicated using the {@link KeyPairEventListener#added(KeyPairResource, String)}
     * callback.
     *
     * @param keyPair the old (outgoing) {@link KeyPairResource}
     */
    default void revoked(KeyPairResource keyPair) {

    }

    /**
     * A {@link KeyPairResource} was activated. Only keys that are in the {@link org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState#ACTIVATED} state can be used for signing.
     *
     * @param activatedKeyPair the {@link KeyPairResource} that was activated
     * @param type             Verification type specifying the cryptographic context in which the public key is used, e.g. JsonWebKey2020...
     */
    default void activated(KeyPairResource activatedKeyPair, String type) {

    }
}
