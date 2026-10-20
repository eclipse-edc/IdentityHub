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

package org.eclipse.edc.identityhub.api;

import org.eclipse.edc.api.authentication.filter.JwtValidatorFilter;
import org.eclipse.edc.api.authentication.filter.ServicePrincipalAuthenticationFilter;
import org.eclipse.edc.api.authorization.filter.ScopeBasedAccessFeature;
import org.eclipse.edc.identityhub.api.authentication.Oauth2TokenRules;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityApiScopes;
import org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext;
import org.eclipse.edc.keys.resolver.JwksPublicKeyResolver;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.web.spi.WebService;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;

import static org.eclipse.edc.identityhub.api.Oauth2JwtAuthenticationExtension.NAME;

@Extension(NAME)
public class Oauth2JwtAuthenticationExtension implements ServiceExtension {
    public static final String NAME = "Identity API OAuth2/JWT Authentication Extension";
    public static final String CONFIG_ISSUER = Oauth2TokenRules.CONFIG_ISSUER;
    public static final String CONFIG_AUDIENCE = Oauth2TokenRules.CONFIG_AUDIENCE;
    public static final String CONFIG_CACHE_VALIDITY = "edc.iam.oauth2.jwks.cache.validity";
    public static final String CONFIG_JWKS_URL = "edc.iam.oauth2.jwks.url";
    public static final String CONFIG_VALIDITY_LEEWAY = "edc.iam.oauth2.validity.leeway";
    private static final long FIVE_MINUTES = 1000 * 60 * 5;
    @Inject
    private WebService webService;
    @Inject
    private ParticipantContextService participantContextService;
    @Configuration
    private OauthConfiguration oauthConfiguration;
    @Inject
    private Clock clock;
    @Inject
    private TokenValidationService tokenValidationService;
    @Inject
    private KeyParserRegistry keyParserRegistry;
    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var alias = IdentityHubApiContext.IDENTITY;

        validateConfig(oauthConfiguration);

        webService.registerResource(alias, new ScopeBasedAccessFeature());
        webService.registerResource(alias, new ServicePrincipalAuthenticationFilter(participantContextService, IdentityApiScopes.ADMIN));

        var resolver = JwksPublicKeyResolver.create(keyParserRegistry, oauthConfiguration.jwksUrl(), monitor, oauthConfiguration.cacheValidityInMillis());
        var rules = Oauth2TokenRules.create(clock, oauthConfiguration.validityLeewaySec(), oauthConfiguration.expectedIssuer(), oauthConfiguration.expectedAudience(), monitor);
        webService.registerResource(alias, new JwtValidatorFilter(tokenValidationService, resolver, rules));
    }

    private void validateConfig(OauthConfiguration oauthConfiguration) {
        if (oauthConfiguration.cacheValidityInMillis < 0) {
            throw new EdcException("Config value '%s' must be greater than 0".formatted(CONFIG_CACHE_VALIDITY));
        }
        if (oauthConfiguration.validityLeewaySec < 0) {
            throw new EdcException("Config value '%s' must be greater than 0".formatted(CONFIG_VALIDITY_LEEWAY));
        }

        try {
            new URL(oauthConfiguration.jwksUrl);
        } catch (MalformedURLException e) {
            throw new EdcException("Config value '%s' must be a valid URL".formatted(CONFIG_JWKS_URL), e);
        }
    }

    @Settings
    record OauthConfiguration(
            @Setting(key = CONFIG_ISSUER, description = "Expected value for the 'iss' claim for tokens coming from the OAuth2 server. If this is omitted, the 'iss' claim is not validated", required = false)
            String expectedIssuer,
            @Setting(key = CONFIG_AUDIENCE, description = "Audience that the 'aud' claim of tokens coming from the OAuth2 server must contain. If this is omitted, the 'aud' claim is not validated", required = false)
            String expectedAudience,
            @Setting(key = CONFIG_JWKS_URL, description = "Absolute URL where the JWKS of the OAuth2 server is hosted")
            String jwksUrl,
            @Setting(key = CONFIG_CACHE_VALIDITY, description = "Time (in ms) that cached JWKS are cached", defaultValue = "" + FIVE_MINUTES, min = 0)
            long cacheValidityInMillis,
            @Setting(key = CONFIG_VALIDITY_LEEWAY, description = "Leeway (in sec) to allow when validating token time-based claims", defaultValue = "5", min = 0)
            int validityLeewaySec
    ) {
    }
}
