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

package org.eclipse.edc.identityhub.spi.events.keypair;

import org.eclipse.edc.identityhub.spi.model.KeyPairResource;
import org.eclipse.edc.spi.observe.Observable;

/**
 * Interface implemented by listeners registered to observe key pair resource changes via {@link Observable#registerListener}.
 * The listener must be called after the state changes are persisted.
 */
public interface KeyPairEventListener {

    /**
     * A {@link KeyPairResource} was added to a particular {@link org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext}. This could happen either
     * by simply adding a keypair, or after a keypair was revoked, and a successor was specified.
     *
     * @param keypair The new (added) key pair
     */
    default void added(KeyPairResource keypair) {

    }

    /**
     * A {@link KeyPairResource} was rotated (=phased out). If the rotation was done with a successor keypair, this would be communicated using the {@link KeyPairEventListener#added(KeyPairResource)}
     * callback.
     *
     * @param keyPair the old (outgoing) {@link KeyPairResource}
     */
    default void rotated(KeyPairResource keyPair) {

    }

    /**
     * A {@link KeyPairResource} was revoked (=deleted). If the revocation was done with a successor keypair, this would be communicated using the {@link KeyPairEventListener#added(KeyPairResource)}
     * callback.
     *
     * @param keyPair the old (outgoing) {@link KeyPairResource}
     */
    default void revoked(KeyPairResource keyPair) {

    }
}
