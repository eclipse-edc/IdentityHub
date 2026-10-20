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

package org.eclipse.edc.identityhub.core.services.verifiablecredential;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialStatusCheckService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;


public class CredentialStatusCheckServiceImpl implements CredentialStatusCheckService {
    private static final String SUSPENSION = "suspension";
    private static final String REVOCATION = "revocation";
    private final RevocationServiceRegistry revocationServiceRegistry;
    private final Clock clock;


    public CredentialStatusCheckServiceImpl(RevocationServiceRegistry revocationServiceRegistry, Clock clock) {
        this.revocationServiceRegistry = revocationServiceRegistry;
        this.clock = clock;
    }

    @Override
    public Result<VcStatus> checkStatus(VerifiableCredentialResource credential) {
        // a superseded credential was replaced by a re-issued one and stays EXPIRED permanently, even while its own
        // validity dates would still make it usable. It is never presented again, so its revocation status is irrelevant.
        if (credential.isSuperseded()) {
            return success(VcStatus.EXPIRED);
        }

        Set<String> statusPurposes;
        try {
            statusPurposes = fetchStatusPurposes(credential);
        } catch (EdcException ex) {
            return failure(ex.getMessage());
        }

        if (statusPurposes.contains(REVOCATION)) {
            return success(VcStatus.REVOKED); //irreversible, cannot be overwritten
        }
        if (statusPurposes.contains(SUSPENSION)) {
            return success(VcStatus.SUSPENDED);
        }

        if (isExpired(credential)) {
            return success(VcStatus.EXPIRED);
        }
        VcStatus targetStatus;
        if (isNotYetValid(credential)) {
            targetStatus = VcStatus.NOT_YET_VALID;
        } else {
            targetStatus = VcStatus.ISSUED;
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

    // returns the purposes of all status entries that are set, e.g. "revocation" or "suspension"
    private Set<String> fetchStatusPurposes(VerifiableCredentialResource resource) {
        var cred = resource.getVerifiableCredential().credential();
        if (cred == null) {
            return Set.of();
        }
        var status = revocationServiceRegistry.getRevocationStatus(cred)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
        return parseStatusPurposes(status);
    }

    /**
     * The revocation service reports the purposes of all status entries of a credential that are set as one string,
     * separated by commas, e.g. "revocation, suspension" for a credential that has several status entries.
     */
    private Set<String> parseStatusPurposes(@Nullable String status) {
        if (status == null) {
            return Set.of();
        }
        return Arrays.stream(status.split(","))
                .map(purpose -> purpose.trim().toLowerCase(Locale.ROOT))
                .filter(purpose -> !purpose.isEmpty())
                .collect(Collectors.toSet());
    }
}
