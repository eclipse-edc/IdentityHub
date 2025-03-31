/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.verifiablecredentials.events;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOffer;
import org.eclipse.edc.spi.observe.Observable;

/**
 * Interface implemented by listeners registered to observe credential offers via {@link Observable#registerListener}.
 * The listener must be called after the CredentialOffer has been persisted.
 */
public interface CredentialOfferListener {
    default void received(CredentialOffer credentialOffer) {

    }
}
