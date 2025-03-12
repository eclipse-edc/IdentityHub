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

package org.eclipse.edc.issuerservice.spi.credentials.statuslist;

import org.eclipse.edc.spi.result.Result;

/**
 * Publishes a status list credential (Bitstring Statuslist, StatusList2021,...) to a publicly available location.
 * In many cases this will be a simple static web resource
 */
public interface StatusListCredentialPublisher {

    /**
     * Publishes the given status list credential.
     *
     * @param participantContextId   The ID of the participant context that represents the issuer
     * @param statusListCredentialId The ID of the {@link org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource} that contains the status list credential.
     * @return success, if the publishing was successful, a failure otherwise, for example, because the given resource is not a status list credential. Returns a URL to the published resource.
     */
    Result<String> publish(String participantContextId, String statusListCredentialId);


    /**
     * Determines, whether the given {@link org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource} can
     * be handled by this publisher. Reasons why this might not be the case include:
     * <ul>
     *     <li>invalid format: the credential is in a format that the publisher can't handle (e.g. binary data)</li>
     *     <li>invalid type: the credential is not a status list credential</li>
     * </ul>
     *
     * @param participantContextId   The ID of the participant context that represents the issuer
     * @param statusListCredentialId The ID of the {@link org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource} that contains the status list credential.
     * @return true if can be handled, false otherwise. This does not necessarily mean, that the credential can be <em>published</em> as well.
     */
    boolean canHandle(String participantContextId, String statusListCredentialId);


    /**
     * Un-Publishes the given status list credential, i.e. removes it from the public location.
     *
     * @param participantContextId   The ID of the participant context that represents the issuer
     * @param statusListCredentialId The ID of the {@link org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource} that contains the status list credential.
     * @return success, if the un-publishing was successful, a failure otherwise, for example, because the given resource was not published.
     */
    Result<Void> unpublish(String participantContextId, String statusListCredentialId);
}
