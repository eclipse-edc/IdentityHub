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

package org.eclipse.edc.identityhub.token.verification;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class AudienceValidationRule implements TokenValidationRule {
    private final String expectedAudience;

    public AudienceValidationRule(String expectedAudience) {
        this.expectedAudience = expectedAudience;
    }

    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        List<?> audiences = toVerify.getListClaim("aud");
        if (audiences.isEmpty()) {
            return Result.failure("Required audience (aud) claim is missing in token");
        } else {
            return !audiences.contains(this.expectedAudience) ? Result.failure("Token audience claim (aud -> %s) did not contain expected audience: %s".formatted(audiences.stream().map(Object::toString).toList(), this.expectedAudience)) :
                    Result.success();
        }
    }
}
