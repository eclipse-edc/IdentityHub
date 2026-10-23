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
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpProfileRegistry;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpProfile;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerEqualsSubjectRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC2_0_JOSE;
import static org.eclipse.edc.identityhub.protocols.dcp.issuer.DcpIssuerCoreExtension.DCP_ISSUER_SELF_ISSUED_TOKEN_CONTEXT;
import static org.eclipse.edc.identityhub.protocols.dcp.issuer.DcpIssuerCoreExtension.ISSUER_PROFILE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class DcpIssuerCoreExtensionTest {


    private final TokenValidationRulesRegistry tokenValidationRulesRegistry = mock();
    private final DcpProfileRegistry profileRegistry = mock();
    private final CredentialGeneratorRegistry credentialGeneratorRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(TokenValidationRulesRegistry.class, tokenValidationRulesRegistry);
        context.registerService(DcpProfileRegistry.class, profileRegistry);
        context.registerService(CredentialGeneratorRegistry.class, credentialGeneratorRegistry);
        when(profileRegistry.getProfile("vc20-bssl/jwt")).thenReturn(new DcpProfile("vc20-bssl/jwt", VC2_0_JOSE, "BitstringStatusListEntry"));
        when(profileRegistry.getProfile("vc11-sl2021/jwt")).thenReturn(new DcpProfile("vc11-sl2021/jwt", VC1_0_JWT, "StatusList2021Entry"));
        when(credentialGeneratorRegistry.supports(any())).thenReturn(true);
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

    @Test
    void issuerProfile_shouldDefaultToVcdm20WithBitstringStatusList(ServiceExtensionContext context, ObjectFactory factory) {
        var extension = factory.constructInstance(DcpIssuerCoreExtension.class);

        extension.prepare();

        assertThat(extension.issuerProfile().get().name()).isEqualTo("vc20-bssl/jwt");
    }

    @Test
    void issuerProfile_shouldBeConfigurable(ServiceExtensionContext context, ObjectFactory factory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(ISSUER_PROFILE, "vc11-sl2021/jwt")));
        var extension = factory.constructInstance(DcpIssuerCoreExtension.class);

        extension.prepare();

        assertThat(extension.issuerProfile().get().format()).isEqualTo(VC1_0_JWT);
    }

    @Test
    void prepare_whenProfileNotRegistered_shouldFail(ServiceExtensionContext context, ObjectFactory factory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(ISSUER_PROFILE, "vc99-unknown/jwt")));
        var extension = factory.constructInstance(DcpIssuerCoreExtension.class);

        assertThatThrownBy(extension::prepare).isInstanceOf(EdcException.class).hasMessageContaining("'vc99-unknown/jwt'");
        assertThatThrownBy(() -> extension.issuerProfile().get()).isInstanceOf(EdcException.class);
    }

    @Test
    void prepare_whenFormatOfProfileCannotBeGenerated_shouldFail(ServiceExtensionContext context, ObjectFactory factory) {
        when(credentialGeneratorRegistry.supports(VC2_0_JOSE)).thenReturn(false);
        var extension = factory.constructInstance(DcpIssuerCoreExtension.class);

        assertThatThrownBy(extension::prepare).isInstanceOf(EdcException.class).hasMessageContaining("VC2_0_JOSE");
    }
}
