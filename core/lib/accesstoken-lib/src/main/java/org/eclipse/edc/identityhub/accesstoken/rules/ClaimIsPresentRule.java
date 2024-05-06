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

package org.eclipse.edc.identityhub.accesstoken.rules;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Rule to assert that a particular claim is present on the {@link ClaimToken} with actually verifying its value.
 */
public class ClaimIsPresentRule implements TokenValidationRule {
    private final String requiredClaim;

    public ClaimIsPresentRule(String requiredClaim) {
        this.requiredClaim = requiredClaim;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken claimToken, @Nullable Map<String, Object> additional) {
        return claimToken.getStringClaim(requiredClaim) != null ?
                Result.success() :
                Result.failure("Required claim '%s' not present on token.".formatted(requiredClaim));
    }
}
