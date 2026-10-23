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
     * Create the status list credential url given the base url the the VC resource. The url ends with the ID of the
     * credential, or with the last path segment of that ID if the credential is identified by its URL.
     *
     * @param baseUrl the baseUrl.
     * @param verifiableCredentialResource the credentials resource.
     * @return the status list credential url.
     */
    static String createUrl(String baseUrl, VerifiableCredentialResource verifiableCredentialResource) {
        var id = verifiableCredentialResource.getVerifiableCredential().credential().getId();
        return createUrl(baseUrl, id.startsWith("http://") || id.startsWith("https://") ? extractIdFromUrl(URI.create(id)) : id);
    }

    /**
     * Create the status list credential url given the base url and the ID of the status list.
     *
     * @param baseUrl the baseUrl.
     * @param statusListId the ID of the status list, which is the last path segment of the url.
     * @return the status list credential url.
     */
    static String createUrl(String baseUrl, String statusListId) {
        var base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return base + statusListId;
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
