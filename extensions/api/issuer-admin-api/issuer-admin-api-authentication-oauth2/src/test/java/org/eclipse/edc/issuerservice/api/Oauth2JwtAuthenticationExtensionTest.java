/*
 *  Copyright (c) 2025 Metaform Systems Inc.
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

package org.eclipse.edc.issuerservice.api;

import org.eclipse.edc.api.authentication.filter.JwtValidatorFilter;
import org.eclipse.edc.api.authentication.filter.ServicePrincipalAuthenticationFilter;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.web.spi.WebService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.issuerservice.api.Oauth2JwtAuthenticationExtension.CONFIG_CACHE_VALIDITY;
import static org.eclipse.edc.issuerservice.api.Oauth2JwtAuthenticationExtension.CONFIG_ISSUER;
import static org.eclipse.edc.issuerservice.api.Oauth2JwtAuthenticationExtension.CONFIG_JWKS_URL;
import static org.eclipse.edc.issuerservice.api.Oauth2JwtAuthenticationExtension.CONFIG_VALIDITY_LEEWAY;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class Oauth2JwtAuthenticationExtensionTest {

    private final WebService webService = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(WebService.class, webService);
    }

    @Test
    void verifyResourceRegistration(ServiceExtensionContext context, ObjectFactory factory) {
        var mockedConfig = ConfigFactory.fromMap(Map.of(
                CONFIG_ISSUER, "foobar",
                CONFIG_JWKS_URL, "https://foo.bar.com"
        ));
        when(context.getConfig()).thenReturn(mockedConfig);


        var extension = factory.constructInstance(Oauth2JwtAuthenticationExtension.class);
        extension.initialize(context);

        verify(webService).registerResource(eq(IdentityHubApiContext.IDENTITY), isA(ServicePrincipalAuthenticationFilter.class));
        verify(webService).registerResource(eq(IdentityHubApiContext.IDENTITY), isA(JwtValidatorFilter.class));
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(IllegalConfigProvider.class)
    void verifyRequiredConfig(String desc, Config illegalConfig, ServiceExtensionContext context, ObjectFactory factory) {
        when(context.getConfig()).thenReturn(illegalConfig);
        assertThatThrownBy(() -> {
            var ext = factory.constructInstance(Oauth2JwtAuthenticationExtension.class);
            ext.initialize(context);
        }).isInstanceOf(EdcException.class);
    }

    private static class IllegalConfigProvider implements ArgumentsProvider {
        @Override
        public @NotNull Stream<? extends Arguments> provideArguments(@NotNull ParameterDeclarations parameters,
                                                                     @NotNull ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of("no JWKS URL", ConfigFactory.fromMap(Map.of(CONFIG_ISSUER, "issuer"))),
                    Arguments.of("invalid JWKS URL", ConfigFactory.fromMap(Map.of(CONFIG_ISSUER, "issuer", CONFIG_JWKS_URL, "this  is-not-a-url"))),
                    Arguments.of("invalid (negative) leeway", ConfigFactory.fromMap(Map.of(CONFIG_ISSUER, "issuer", CONFIG_JWKS_URL, "https://valid.url", CONFIG_VALIDITY_LEEWAY, "-3"))),
                    Arguments.of("invalid (negative) cache validity", ConfigFactory.fromMap(Map.of(CONFIG_ISSUER, "issuer", CONFIG_JWKS_URL, "https://valid.url", CONFIG_CACHE_VALIDITY, "-3")))
            );
        }
    }
}