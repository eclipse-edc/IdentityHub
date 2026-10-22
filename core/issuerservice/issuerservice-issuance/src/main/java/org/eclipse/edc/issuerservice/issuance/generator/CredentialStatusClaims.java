/*
 *  Copyright (c) 2026 Metaform Systems Inc.
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

package org.eclipse.edc.issuerservice.issuance.generator;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.eclipse.edc.issuerservice.issuance.generator.Constants.ID;
import static org.eclipse.edc.issuerservice.issuance.generator.Constants.TYPE;

/**
 * Represents the {@link CredentialStatus} entries of a {@link VerifiableCredential} as the claims of a signed credential.
 */
class CredentialStatusClaims {

    /**
     * The claims of all credential status entries of the credential.
     *
     * @param credential the credential
     * @return empty if the credential has no status, a single object if it has one status entry, or a list of objects if
     *         it has several
     */
    Optional<Object> of(VerifiableCredential credential) {
        var statuses = credential.getCredentialStatus();
        if (statuses.isEmpty()) {
            return Optional.empty();
        }
        if (statuses.size() == 1) {
            return Optional.of(toClaims(statuses.get(0)));
        }
        return Optional.of(statuses.stream().map(this::toClaims).toList());
    }

    private Map<String, Object> toClaims(CredentialStatus status) {
        var statusMap = new HashMap<String, Object>(Map.of(ID, status.id(), TYPE, status.type()));
        statusMap.putAll(status.additionalProperties());
        return statusMap;
    }
}
