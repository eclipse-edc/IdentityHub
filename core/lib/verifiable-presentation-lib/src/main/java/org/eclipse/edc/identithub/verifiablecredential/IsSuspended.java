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
 * Checks is a credential is suspended. An {@link EdcException} is thrown if the revocation check fails.
 */
public class IsSuspended implements Function<VerifiableCredential, Boolean> {
    private static final String SUSPENSION = "suspension";
    private final RevocationListService revocationListService;

    public IsSuspended(RevocationListService revocationListService) {
        this.revocationListService = revocationListService;
    }

    @Override
    public Boolean apply(VerifiableCredential verifiableCredential) {
        return revocationListService.getStatusPurpose(verifiableCredential)
                .map(SUSPENSION::equalsIgnoreCase)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }
}
