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
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialStatusCheckService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.spi.result.Result;

import java.time.Clock;
import java.util.List;
import java.util.function.Function;

import static org.eclipse.edc.spi.result.Result.success;

public class CredentialStatusCheckServiceImpl implements CredentialStatusCheckService {
    private final RevocationListService revocationListService;
    private final Clock clock;

    public CredentialStatusCheckServiceImpl(RevocationListService revocationListService, Clock clock) {
        this.revocationListService = revocationListService;
        this.clock = clock;
    }

    @Override
    public Result<VcStatus> checkStatus(VerifiableCredentialResource resource) {
        var statusMap = List.of(
                new Pair(new IsExpired(clock), VcStatus.EXPIRED),
                new Pair(new IsNotYetValid(clock), VcStatus.NOT_YET_VALID),
                new Pair(new IsRevoked(revocationListService), VcStatus.REVOKED),
                new Pair(new IsSuspended(revocationListService), VcStatus.SUSPENDED)
        );

        var cred = resource.getVerifiableCredential().credential();
        return statusMap.stream()
                .filter(p -> p.rule().apply(cred))
                .map(p -> success(p.targetState()))
                .findFirst()
                // assume ISSUED only if the credential is present, use previous state otherwise
                .orElse(success(getFallbackStatus(resource)));
    }

    /**
     * returns the state if the check _wasn't_ successful, e.g. an unsuccessful {@link IsExpired} check means, the credential
     * is _not_ expired. Reversible states, i.e. {@link VcStatus#NOT_YET_VALID} and {@link VcStatus#SUSPENDED} will always return to
     * {@link VcStatus#ISSUED}, because that means that the credential "isn't suspended anymore". In all other cases the
     * resource's current status is used.
     */
    private VcStatus getFallbackStatus(VerifiableCredentialResource resource) {
        if (resource.getStateAsEnum() == VcStatus.NOT_YET_VALID || resource.getStateAsEnum() == VcStatus.SUSPENDED) {
            return VcStatus.ISSUED;
        }
        return resource.getStateAsEnum();
    }

    private record Pair(Function<VerifiableCredential, Boolean> rule, VcStatus targetState) {
    }
}
