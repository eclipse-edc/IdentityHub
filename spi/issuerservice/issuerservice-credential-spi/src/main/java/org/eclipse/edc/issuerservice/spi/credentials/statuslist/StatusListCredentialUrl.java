/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.spi.credentials.statuslist;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;

import java.net.URI;

/**
 * Describe the status list credential url format
 */
public interface StatusListCredentialUrl {

    /**
     * Create the status list credential url given the base url the the VC resource.
     *
     * @param baseUrl the baseUrl.
     * @param verifiableCredentialResource the credentials resource.
     * @return the status list credential url.
     */
    static String createUrl(String baseUrl, VerifiableCredentialResource verifiableCredentialResource) {
        var base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return base + verifiableCredentialResource.getVerifiableCredential().credential().getId();
    }

    /**
     * Extract the credential url from the status list
     *
     * @param uri the status list url.
     * @return the credential id.
     */
    static String extractIdFromUrl(URI uri) {
        var split = uri.getPath().split("/");
        return split[split.length - 1];
    }

}
