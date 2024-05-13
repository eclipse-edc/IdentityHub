/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identithub.verifiablecredential;

import org.eclipse.edc.iam.verifiablecredentials.spi.RevocationListService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.spi.EdcException;

import java.util.function.Function;

/**
 * Checks is a credential is revoked. An {@link EdcException} is thrown if the revocation check fails.
 */
class IsRevoked implements Function<VerifiableCredential, Boolean> {
    private static final String REVOCATION = "revocation";
    private final RevocationListService revocationListService;

    IsRevoked(RevocationListService revocationListService) {
        this.revocationListService = revocationListService;
    }

    @Override
    public Boolean apply(VerifiableCredential verifiableCredential) {
        return revocationListService.getStatusPurpose(verifiableCredential)
                .map(REVOCATION::equalsIgnoreCase)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }
}
