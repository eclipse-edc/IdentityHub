/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.api.authentication;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.token.rules.AudienceValidationRule;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.rules.IssuerEqualsValidationRule;
import org.eclipse.edc.token.rules.NotBeforeValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the rules with which the OAuth2 authentication extensions of the admin APIs validate bearer tokens.
 */
public final class Oauth2TokenRules {
    public static final String CONFIG_ISSUER = "edc.iam.oauth2.issuer";
    public static final String CONFIG_AUDIENCE = "edc.iam.oauth2.audience";

    private Oauth2TokenRules() {
    }

    /**
     * Creates the token validation rules. The issuer and audience rules are only added when the respective value is
     * configured, and a warning is logged for each one that is not.
     *
     * @param clock             the clock for the time-based rules
     * @param validityLeewaySec the leeway in seconds for the time-based rules
     * @param expectedIssuer    the expected 'iss' claim, or null to not check it
     * @param expectedAudience  the audience the 'aud' claim must contain, or null to not check it
     * @param monitor           the monitor to log warnings to
     */
    public static List<TokenValidationRule> create(Clock clock, int validityLeewaySec, @Nullable String expectedIssuer, @Nullable String expectedAudience, Monitor monitor) {
        var rules = new ArrayList<TokenValidationRule>();
        rules.add(new NotBeforeValidationRule(clock, validityLeewaySec, true));
        rules.add(new ExpirationIssuedAtValidationRule(clock, validityLeewaySec, false));

        if (expectedIssuer != null) {
            rules.add(new IssuerEqualsValidationRule(expectedIssuer));
        } else {
            monitor.warning("Config value '%s' not set. Tokens will not be validated against the issuer claim.".formatted(CONFIG_ISSUER));
        }

        if (expectedAudience != null) {
            rules.add(new AudienceValidationRule(expectedAudience));
        } else {
            monitor.warning("Config value '%s' not set. Tokens will not be validated against the audience claim.".formatted(CONFIG_AUDIENCE));
        }
        return rules;
    }
}
