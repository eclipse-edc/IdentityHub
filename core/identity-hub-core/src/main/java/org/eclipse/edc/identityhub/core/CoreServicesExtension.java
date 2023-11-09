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

package org.eclipse.edc.identityhub.core;

import org.eclipse.edc.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.identitytrust.validation.SelfIssuedIdTokenValidator;
import org.eclipse.edc.identityhub.spi.ScopeToCriterionTransformer;
import org.eclipse.edc.identityhub.spi.resolution.CredentialQueryResolver;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.verification.AccessTokenVerifier;
import org.eclipse.edc.identityhub.token.verification.AccessTokenVerifierImpl;
import org.eclipse.edc.identitytrust.validation.JwtValidator;
import org.eclipse.edc.identitytrust.verification.JwtVerifier;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.verification.jwt.SelfIssuedIdTokenVerifier;

import java.net.URISyntaxException;

import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.IATP_CONTEXT_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.PRESENTATION_EXCHANGE_URL;

/**
 * This extension provides some core services for the IdentityHub, such as:
 * <ul>
 *     <li>an {@link AccessTokenVerifier}</li>
 *     <li>a {@link JwtValidator}</li>
 *     <li>a {@link JwtVerifier}</li>
 * </ul>
 */
@Extension(value = "Core Services extension")
public class CoreServicesExtension implements ServiceExtension {

    @Setting(value = "Configure this IdentityHub's DID", required = true)
    public static final String OWN_DID_PROPERTY = "edc.ih.iam.id";
    public static final String PRESENTATION_EXCHANGE_V_1_JSON = "presentation-exchange.v1.json";
    public static final String PRESENTATION_QUERY_V_08_JSON = "presentation-query.v08.json";
    private JwtVerifier jwtVerifier;
    private JwtValidator jwtValidator;
    @Inject
    private DidResolverRegistry didResolverRegistry;

    @Inject
    private PublicKeyWrapper identityHubPublicKey;

    @Inject
    private JsonLd jsonLd;

    @Inject
    private CredentialStore credentialStore;

    @Inject
    private ScopeToCriterionTransformer transformer;

    @Override
    public void initialize(ServiceExtensionContext context) {
        // Setup API
        cacheContextDocuments(getClass().getClassLoader());
    }

    @Provider
    public AccessTokenVerifier createAccessTokenVerifier(ServiceExtensionContext context) {
        return new AccessTokenVerifierImpl(getJwtVerifier(), getJwtValidator(), getOwnDid(context), identityHubPublicKey);
    }

    @Provider
    public JwtValidator getJwtValidator() {
        if (jwtValidator == null) {
            jwtValidator = new SelfIssuedIdTokenValidator();
        }
        return jwtValidator;
    }

    @Provider
    public JwtVerifier getJwtVerifier() {
        if (jwtVerifier == null) {
            jwtVerifier = new SelfIssuedIdTokenVerifier(didResolverRegistry);
        }
        return jwtVerifier;
    }

    @Provider
    public CredentialQueryResolver createCredentialQueryResolver() {
        return new CredentialQueryResolverImpl(credentialStore, transformer);
    }

    private String getOwnDid(ServiceExtensionContext context) {
        return context.getConfig().getString(OWN_DID_PROPERTY);
    }

    private void cacheContextDocuments(ClassLoader classLoader) {
        try {
            jsonLd.registerCachedDocument(PRESENTATION_EXCHANGE_URL, classLoader.getResource(PRESENTATION_EXCHANGE_V_1_JSON).toURI());
            jsonLd.registerCachedDocument(IATP_CONTEXT_URL, classLoader.getResource(PRESENTATION_QUERY_V_08_JSON).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
