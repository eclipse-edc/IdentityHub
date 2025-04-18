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

package org.eclipse.edc.issuerservice.publisher;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListCredentialPublisher;
import org.eclipse.edc.spi.result.Result;

public class LocalCredentialPublisher implements StatusListCredentialPublisher {
    private final String baseUrl;

    public LocalCredentialPublisher(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    @Override
    public Result<String> publish(VerifiableCredentialResource verifiableCredentialResource) {
        var url = baseUrl + verifiableCredentialResource.getVerifiableCredential().credential().getId();
        return Result.success(url);
    }

}
