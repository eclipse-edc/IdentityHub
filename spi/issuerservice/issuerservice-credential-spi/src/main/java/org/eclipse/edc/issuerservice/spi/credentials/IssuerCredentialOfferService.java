/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.spi.credentials;

import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Collection;

/**
 * Service to be used on the issuer side to send credential offers to holders
 */
public interface IssuerCredentialOfferService {
    String OFFER_ENDPOINT = "/offers";

    /**
     * Sends a {@code CredentialOfferMessage} to the given holder to inform them about the availability of a credential
     *
     * @param participantContextId the ID of the current issuer participant context
     * @param holderId             the ID of the holder.
     * @param credentialObjectIds  a list of IDs of the {@code CredentialObject} objects that should be offered to the holder.
     * @return a result to indicate whether the {@code CredentialOfferMessage} was sent successfully.
     */
    ServiceResult<Void> sendCredentialOffer(String participantContextId, String holderId, Collection<String> credentialObjectIds);
}
