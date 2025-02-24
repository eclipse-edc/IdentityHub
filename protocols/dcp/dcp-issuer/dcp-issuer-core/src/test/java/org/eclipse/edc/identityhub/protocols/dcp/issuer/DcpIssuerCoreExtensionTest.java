/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.protocols.dcp.issuer;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerEqualsSubjectRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.protocols.dcp.issuer.DcpIssuerCoreExtension.DCP_ISSUER_SELF_ISSUED_TOKEN_CONTEXT;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
public class DcpIssuerCoreExtensionTest {


    private final TokenValidationRulesRegistry tokenValidationRulesRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(TokenValidationRulesRegistry.class, tokenValidationRulesRegistry);
    }

    @Test
    void verifyProviders(ServiceExtensionContext context, ObjectFactory factory) {
        var extension = factory.constructInstance(DcpIssuerCoreExtension.class);
        assertThat(extension.createIssuerService()).isInstanceOf(DcpIssuerServiceImpl.class);
        assertThat(extension.createTokenVerifier()).isInstanceOf(DcpHolderTokenVerifierImpl.class);
    }

    @Test
    void verifyTokenValidationRules(ServiceExtensionContext context, ObjectFactory factory) {
        var extension = factory.constructInstance(DcpIssuerCoreExtension.class);
        extension.initialize(context);

        verify(tokenValidationRulesRegistry).addRule(eq(DCP_ISSUER_SELF_ISSUED_TOKEN_CONTEXT), isA(IssuerEqualsSubjectRule.class));
        verify(tokenValidationRulesRegistry).addRule(eq(DCP_ISSUER_SELF_ISSUED_TOKEN_CONTEXT), isA(ExpirationIssuedAtValidationRule.class));
    }
}
