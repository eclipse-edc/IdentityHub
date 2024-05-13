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

public class IsExpired implements CredentialValidationRule {
    private final Clock clock;

    public IsExpired(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Result<Void> apply(VerifiableCredential credential) {
        var now = clock.instant();
        if (credential.getExpirationDate() != null && credential.getExpirationDate().isBefore(now)) {
            return Result.failure("Credential expired.");
        }
        return Result.success();
    }
}
