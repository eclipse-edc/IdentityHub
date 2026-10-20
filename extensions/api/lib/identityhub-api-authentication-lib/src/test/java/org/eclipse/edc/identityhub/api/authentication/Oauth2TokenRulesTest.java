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
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.api.authentication.Oauth2TokenRules.CONFIG_AUDIENCE;
import static org.eclipse.edc.identityhub.api.authentication.Oauth2TokenRules.CONFIG_ISSUER;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class Oauth2TokenRulesTest {

    private final Monitor monitor = mock();
    private final Clock clock = Clock.systemUTC();

    @Test
    void create_withIssuerAndAudience() {
        var rules = Oauth2TokenRules.create(clock, 5, "issuer", "audience", monitor);

        assertThat(rules).hasExactlyElementsOfTypes(NotBeforeValidationRule.class, ExpirationIssuedAtValidationRule.class,
                IssuerEqualsValidationRule.class, AudienceValidationRule.class);
        verifyNoInteractions(monitor);
    }

    @Test
    void create_withoutIssuer_warns() {
        var rules = Oauth2TokenRules.create(clock, 5, null, "audience", monitor);

        assertThat(rules).hasExactlyElementsOfTypes(NotBeforeValidationRule.class, ExpirationIssuedAtValidationRule.class,
                AudienceValidationRule.class);
        verify(monitor).warning(contains(CONFIG_ISSUER));
        verify(monitor, never()).warning(contains(CONFIG_AUDIENCE));
    }

    @Test
    void create_withoutAudience_warns() {
        var rules = Oauth2TokenRules.create(clock, 5, "issuer", null, monitor);

        assertThat(rules).hasExactlyElementsOfTypes(NotBeforeValidationRule.class, ExpirationIssuedAtValidationRule.class,
                IssuerEqualsValidationRule.class);
        verify(monitor).warning(contains(CONFIG_AUDIENCE));
        verify(monitor, never()).warning(contains(CONFIG_ISSUER));
    }
}
