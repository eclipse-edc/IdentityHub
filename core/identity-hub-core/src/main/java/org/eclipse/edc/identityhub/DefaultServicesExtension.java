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

import org.eclipse.edc.iam.identitytrust.spi.verification.SignatureSuiteRegistry;
import org.eclipse.edc.iam.verifiablecredentials.StatusList2021RevocationService;
import org.eclipse.edc.iam.verifiablecredentials.spi.RevocationListService;
import org.eclipse.edc.identityhub.accesstoken.rules.ClaimIsPresentRule;
import org.eclipse.edc.identityhub.defaults.InMemoryCredentialStore;
import org.eclipse.edc.identityhub.defaults.InMemoryKeyPairResourceStore;
import org.eclipse.edc.identityhub.defaults.InMemoryParticipantContextStore;
import org.eclipse.edc.identityhub.defaults.InMemorySignatureSuiteRegistry;
import org.eclipse.edc.identityhub.query.EdcScopeToCriterionTransformer;
import org.eclipse.edc.identityhub.spi.ScopeToCriterionTransformer;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.store.ParticipantContextStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;

import static org.eclipse.edc.identityhub.DefaultServicesExtension.NAME;
import static org.eclipse.edc.identityhub.accesstoken.verification.AccessTokenConstants.ACCESS_TOKEN_SCOPE_CLAIM;
import static org.eclipse.edc.identityhub.accesstoken.verification.AccessTokenConstants.DCP_ACCESS_TOKEN_CONTEXT;
import static org.eclipse.edc.identityhub.accesstoken.verification.AccessTokenConstants.DCP_SELF_ISSUED_TOKEN_CONTEXT;
import static org.eclipse.edc.identityhub.accesstoken.verification.AccessTokenConstants.TOKEN_CLAIM;

@Extension(NAME)
public class DefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "IdentityHub Default Services Extension";
    public static final long DEFAULT_REVOCATION_CACHE_VALIDITY_MILLIS = 15 * 60 * 1000L;
    @Setting(value = "Validity period of cached StatusList2021 credential entries in milliseconds.", defaultValue = DEFAULT_REVOCATION_CACHE_VALIDITY_MILLIS + "", type = "long")
    public static final String REVOCATION_CACHE_VALIDITY = "edc.iam.credential.revocation.cache.validity";
    @Inject
    private TokenValidationRulesRegistry registry;
    @Inject
    private TypeManager typeManager;
    private RevocationListService revocationService;

    @Override
    public String name() {
        return NAME;
    }


    @Override
    public void initialize(ServiceExtensionContext context) {
        var accessTokenRule = new ClaimIsPresentRule(TOKEN_CLAIM);
        registry.addRule(DCP_SELF_ISSUED_TOKEN_CONTEXT, accessTokenRule);

        var scopeIsPresentRule = new ClaimIsPresentRule(ACCESS_TOKEN_SCOPE_CLAIM);
        registry.addRule(DCP_ACCESS_TOKEN_CONTEXT, scopeIsPresentRule);
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
    public RevocationListService createRevocationListService(ServiceExtensionContext context) {
        if (revocationService == null) {
            var validity = context.getConfig().getLong(REVOCATION_CACHE_VALIDITY, DEFAULT_REVOCATION_CACHE_VALIDITY_MILLIS);
            revocationService = new StatusList2021RevocationService(typeManager.getMapper(), validity);
        }
        return revocationService;
    }

    @Provider(isDefault = true)
    public SignatureSuiteRegistry createSignatureSuiteRegistry() {
        return new InMemorySignatureSuiteRegistry();
    }
}
