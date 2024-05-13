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

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.CredentialValidationRule;
import org.eclipse.edc.spi.result.Result;

import java.time.Clock;

public class IsNotYetValid implements CredentialValidationRule {
    private final Clock clock;

    public IsNotYetValid(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Result<Void> apply(VerifiableCredential credential) {
        var now = clock.instant();
        // issuance date can not be null, due to builder validation
        if (credential.getIssuanceDate().isAfter(now)) {
            return Result.failure("Credential is not yet valid.");
        }
        return Result.success();
    }
}
