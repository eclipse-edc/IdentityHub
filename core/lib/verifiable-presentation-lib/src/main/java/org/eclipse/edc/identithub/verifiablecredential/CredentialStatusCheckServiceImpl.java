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
        return statusMap.stream().filter(p -> p.rule().apply(cred))
                .map(p -> success(p.status()))
                .findFirst()
                .orElse(success(resource.getStateAsEnum()));
    }

    private record Pair(Function<VerifiableCredential, Boolean> rule, VcStatus status) {
    }
}
