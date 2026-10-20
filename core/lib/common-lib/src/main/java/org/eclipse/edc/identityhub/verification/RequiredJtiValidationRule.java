/*
 *  Copyright (c) 2026 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.verification;

import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.rules.JtiValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Guards against replayed tokens: the {@code jti} claim must be present, and must not have been used before (see
 * {@link JtiValidationRule}). A token without a {@code jti} would otherwise not be checked at all.
 */
public class RequiredJtiValidationRule implements TokenValidationRule {

    private final JtiValidationRule jtiValidationRule;

    public RequiredJtiValidationRule(JtiValidationStore jtiValidationStore, Monitor monitor) {
        this.jtiValidationRule = new JtiValidationRule(jtiValidationStore, monitor);
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        if (toVerify.getStringClaim(JwtRegisteredClaimNames.JWT_ID) == null) {
            return Result.failure("Required claim '%s' not present on token.".formatted(JwtRegisteredClaimNames.JWT_ID));
        }
        return jtiValidationRule.checkRule(toVerify, additional);
    }
}
