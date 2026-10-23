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

import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.spi.result.Result;

import java.util.Optional;

/**
 * Publishes a status list credential (Bitstring Statuslist, StatusList2021,...) to a publicly available location.
 * In many cases this will be a simple static web resource
 */
public interface StatusListCredentialPublisher {

    /**
     * Publishes the given status list credential.
     *
     * @param verifiableCredentialResource the credential resource
     * @return the url of the published resource, if the publishing was successful, a failure otherwise, for example,
     *         because the given resource is not a status list credential.
     */
    Result<String> publish(VerifiableCredentialResource verifiableCredentialResource);

    /**
     * The URL at which a status list credential will be published, if it is known before the status list credential is
     * published. A status list credential uses this URL as its ID, which the Bitstring Status List requires.
     *
     * @param statusListId the ID by which the publisher derives the URL, e.g. a UUID
     * @return the URL, or empty if the publisher only knows it after publishing. Then the status list credential is
     *         identified by the given ID instead.
     */
    default Optional<String> publicUrl(String statusListId) {
        return Optional.empty();
    }

}
