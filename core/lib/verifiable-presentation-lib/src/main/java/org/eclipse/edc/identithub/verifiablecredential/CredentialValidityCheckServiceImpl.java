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

import org.eclipse.edc.iam.verifiablecredentials.rules.IsNotRevoked;
import org.eclipse.edc.iam.verifiablecredentials.spi.RevocationListService;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.CredentialValidationRule;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialValidityCheckService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.spi.result.Result;

import java.time.Clock;
import java.util.Map;

public class CredentialValidityCheckServiceImpl implements CredentialValidityCheckService {
    private final RevocationListService revocationListService;
    private final Clock clock;

    public CredentialValidityCheckServiceImpl(RevocationListService revocationListService, Clock clock) {
        this.revocationListService = revocationListService;
        this.clock = clock;
    }

    @Override
    public Result<VcStatus> checkStatus(VerifiableCredentialResource resource) {
        // in addition, verify that all VCs are valid
        var defaultConsequences = Map.of(
                new IsExpired(clock), VcStatus.EXPIRED,
                new IsNotYetValid(clock), VcStatus.NOT_YET_VALID,
                new IsNotRevoked(revocationListService), VcStatus.REVOKED,
                new IsNotSuspended(revocationListService), VcStatus.SUSPENDED);


        var filters = allRules.stream();
        var result = filters.reduce(t -> Result.success(), CredentialValidationRule::and).apply(resource.getVerifiableCredential().credential());


        return null;
    }
}
