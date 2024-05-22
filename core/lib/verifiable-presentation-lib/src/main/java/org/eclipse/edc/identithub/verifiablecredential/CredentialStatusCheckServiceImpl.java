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
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialStatusCheckService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;


public class CredentialStatusCheckServiceImpl implements CredentialStatusCheckService {
    private static final String SUSPENSION = "suspension";
    private static final String REVOCATION = "revocation";
    private final RevocationListService revocationListService;
    private final Clock clock;


    public CredentialStatusCheckServiceImpl(RevocationListService revocationListService, Clock clock) {
        this.revocationListService = revocationListService;
        this.clock = clock;
    }

    @Override
    public Result<VcStatus> checkStatus(VerifiableCredentialResource resource) {

        if (isExpired(resource)) {
            return success(VcStatus.EXPIRED);
        }
        VcStatus targetStatus;
        if (isNotYetValid(resource)) {
            targetStatus = VcStatus.NOT_YET_VALID;
        } else {
            targetStatus = VcStatus.ISSUED;
        }

        try {
            if (isRevoked(resource)) {
                return success(VcStatus.REVOKED); //irreversible, cannot be overwritten
            }
            if (isSuspended(resource)) {
                targetStatus = VcStatus.SUSPENDED;
            }

        } catch (EdcException ex) {
            return failure(ex.getMessage());
        }
        return success(targetStatus);
    }

    // returns true if the expiration date is not null and is before NOW
    private boolean isExpired(VerifiableCredentialResource resource) {
        var cred = resource.getVerifiableCredential().credential();

        if (cred == null) {
            return false;
        }

        var now = clock.instant();
        return cred.getExpirationDate() != null && cred.getExpirationDate().isBefore(now);
    }

    // returns true if the issuance date is after NOW
    private boolean isNotYetValid(VerifiableCredentialResource resource) {
        var cred = resource.getVerifiableCredential().credential();
        if (cred == null) {
            return false;
        }

        var now = clock.instant();
        // issuance date can not be null, due to builder validation
        return cred.getIssuanceDate().isAfter(now);
    }

    // returns true if the revocation service returns "suspension"
    private boolean isSuspended(VerifiableCredentialResource resource) {
        return SUSPENSION.equalsIgnoreCase(fetchRevocationStatus(resource));
    }

    // returns true if the revocation service returns "revocation"
    private boolean isRevoked(VerifiableCredentialResource resource) {
        return REVOCATION.equalsIgnoreCase(fetchRevocationStatus(resource));
    }

    @Nullable
    private String fetchRevocationStatus(VerifiableCredentialResource resource) {
        var cred = resource.getVerifiableCredential().credential();
        if (cred == null) {
            return null;
        }
        return revocationListService.getStatusPurpose(cred)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }
}
