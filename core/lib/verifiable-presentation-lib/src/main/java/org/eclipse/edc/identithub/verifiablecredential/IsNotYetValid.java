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

import java.time.Clock;
import java.util.function.Function;

/**
 * Checks if a credential is already valid, i.e. the issuanceDate is before NOW
 */
class IsNotYetValid implements Function<VerifiableCredential, Boolean> {
    private final Clock clock;

    IsNotYetValid(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Boolean apply(VerifiableCredential credential) {
        var now = clock.instant();
        // issuance date can not be null, due to builder validation
        return credential.getIssuanceDate().isAfter(now);
    }
}
