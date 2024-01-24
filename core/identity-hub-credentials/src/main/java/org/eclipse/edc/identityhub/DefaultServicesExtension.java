/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub;

import com.apicatalog.ld.signature.SignatureSuite;
import org.eclipse.edc.identityhub.defaults.EdcScopeToCriterionTransformer;
import org.eclipse.edc.identityhub.defaults.InMemoryCredentialStore;
import org.eclipse.edc.identityhub.defaults.InMemoryKeyPairResourceStore;
import org.eclipse.edc.identityhub.defaults.InMemoryParticipantContextStore;
import org.eclipse.edc.identityhub.spi.ScopeToCriterionTransformer;
import org.eclipse.edc.identityhub.spi.model.IdentityHubConstants;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.store.ParticipantContextStore;
import org.eclipse.edc.identityhub.token.rules.ClaimIsPresentRule;
import org.eclipse.edc.identitytrust.verification.SignatureSuiteRegistry;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.security.signature.jws2020.JwsSignature2020Suite;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;

import java.util.Collection;
import java.util.Map;

import static org.eclipse.edc.identityhub.DefaultServicesExtension.NAME;

@Extension(NAME)
public class DefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "IdentityHub Default Services Extension";
    public static final String IATP_SELF_ISSUED_TOKEN_CONTEXT = "iatp-si";
    public static final String IATP_ACCESS_TOKEN_CONTEXT = "iatp-access-token";
    public static final String ACCESS_TOKEN_CLAIM = "access_token";
    public static final String ACCESS_TOKEN_SCOPE_CLAIM = "scope";

    @Inject
    private TokenValidationRulesRegistry registry;

    @Override
    public String name() {
        return NAME;
    }


    @Override
    public void initialize(ServiceExtensionContext context) {
        var accessTokenRule = new ClaimIsPresentRule(ACCESS_TOKEN_CLAIM);
        registry.addRule(IATP_SELF_ISSUED_TOKEN_CONTEXT, accessTokenRule);

        var scopeIsPresentRule = new ClaimIsPresentRule(ACCESS_TOKEN_SCOPE_CLAIM);
        registry.addRule(IATP_ACCESS_TOKEN_CONTEXT, scopeIsPresentRule);
    }

    @Provider(isDefault = true)
    public CredentialStore createDefaultCredentialStore() {
        return new InMemoryCredentialStore();
    }

    @Provider(isDefault = true)
    public ParticipantContextStore createDefaultParticipantContextStore() {
        return new InMemoryParticipantContextStore();
    }

    @Provider(isDefault = true)
    public KeyPairResourceStore createDefaultKeyPairResourceStore() {
        return new InMemoryKeyPairResourceStore();
    }

    @Provider(isDefault = true)
    public ScopeToCriterionTransformer createScopeTransformer(ServiceExtensionContext context) {
        context.getMonitor().warning("Using the default EdcScopeToCriterionTransformer. This is not intended for production use and should be replaced " +
                "with a specialized implementation for your dataspace");
        return new EdcScopeToCriterionTransformer();
    }

    @Provider(isDefault = true)
    public SignatureSuiteRegistry createSignatureSuiteRegistry() {
        return new SignatureSuiteRegistry() {
            private final Map<String, SignatureSuite> registry = Map.of(IdentityHubConstants.JWS_2020_SIGNATURE_SUITE, new JwsSignature2020Suite(JacksonJsonLd.createObjectMapper()));

            @Override
            public void register(String w3cIdentifier, SignatureSuite suite) {

            }

            @Override
            public SignatureSuite getForId(String w3cIdentifier) {
                return registry.get(w3cIdentifier);
            }

            @Override
            public Collection<SignatureSuite> getAllSuites() {
                return registry.values();
            }
        };
    }
}
