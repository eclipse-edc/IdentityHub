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

package org.eclipse.edc.identityhub.spi.verifiablecredentials.model;

import org.eclipse.edc.spi.entity.StatefulEntity;

import java.util.List;
import java.util.UUID;

import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOfferStatus.from;

public final class CredentialOffer extends StatefulEntity<CredentialOffer> {
    private final String issuer;
    private final List<Object> credentialObjects;

    public CredentialOffer(String id, String issuer, List<Object> credentialObjects, CredentialOfferStatus status) {
        this.id = id;
        this.issuer = issuer;
        this.credentialObjects = credentialObjects;
        this.state = status.code();
    }

    public CredentialOffer(String issuer, List<Object> credentialObjects) {
        this(UUID.randomUUID().toString(), issuer, credentialObjects, CredentialOfferStatus.RECEIVED);
    }

    public String issuer() {
        return issuer;
    }

    public List<Object> credentialObjects() {
        return credentialObjects;
    }


    @Override
    public CredentialOffer copy() {
        return new CredentialOffer(id, issuer, List.copyOf(credentialObjects), from(state));
    }

    @Override
    public String stateAsString() {
        return getStateAsEnum().toString();
    }

    public CredentialOfferStatus getStateAsEnum() {
        return from(state);
    }
}
