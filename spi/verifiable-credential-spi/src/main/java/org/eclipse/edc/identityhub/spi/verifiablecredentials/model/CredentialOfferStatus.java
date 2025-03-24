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

import java.util.Arrays;

/**
 * Enum that indicates the status of a holder-side credential offer. When an issuer sends a {@link CredentialOffer} to the holder,
 * the holder ingests the message for further processing.
 */
public enum CredentialOfferStatus {
    /**
     * CredentialOffer was received and is now present in persistent storage
     */
    RECEIVED(100),
    /**
     * Processing the offer has started. This state may never get persisted, as processing will likely only take a short amount of time.
     */
    PROCESSING(200),
    /**
     * Processing the offer has completed. This does <em>mean</em> that the subsequent action, e.g. an issuance flow, has also completed!
     * This state indicates that any subsequent action <em>has started.</em>
     */
    PROCESSED(300),
    /**
     * The holder rejects the credential offer.
     */
    REJECTED(400);

    private final int code;

    CredentialOfferStatus(int code) {
        this.code = code;
    }

    public static CredentialOfferStatus from(int code) {
        return Arrays.stream(values()).filter(ips -> ips.code == code).findFirst().orElse(null);
    }

    public int code() {
        return code;
    }
}
