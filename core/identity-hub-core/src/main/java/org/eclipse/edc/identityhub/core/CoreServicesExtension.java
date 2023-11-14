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
import org.eclipse.edc.identityhub.core.creators.JwtPresentationCreator;
import org.eclipse.edc.identityhub.core.creators.LdpPresentationCreator;
import org.eclipse.edc.identityhub.spi.ScopeToCriterionTransformer;
import org.eclipse.edc.identityhub.spi.generator.PresentationCreatorRegistry;
import org.eclipse.edc.identityhub.spi.generator.PresentationGenerator;
import org.eclipse.edc.identityhub.spi.model.IdentityHubConstants;
import org.eclipse.edc.identityhub.spi.resolution.CredentialQueryResolver;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.verification.AccessTokenVerifier;
import org.eclipse.edc.identityhub.token.verification.AccessTokenVerifierImpl;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.validation.JwtValidator;
import org.eclipse.edc.identitytrust.verification.JwtVerifier;
import org.eclipse.edc.identitytrust.verification.SignatureSuiteRegistry;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.verifiablecredentials.linkeddata.LdpIssuer;
import org.eclipse.edc.verification.jwt.SelfIssuedIdTokenVerifier;

import java.net.URISyntaxException;
import java.time.Clock;

import static org.eclipse.edc.identityhub.core.CoreServicesExtension.NAME;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.DID_CONTEXT_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.IATP_CONTEXT_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.JWS_2020_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.PRESENTATION_EXCHANGE_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.W3C_CREDENTIALS_URL;

/**
 * This extension provides core services for the IdentityHub that are not intended to be user-replaceable.
 */
@Extension(value = NAME)
public class CoreServicesExtension implements ServiceExtension {

    public static final String NAME = "IdentityHub Core Services Extension";
    @Setting(value = "Configure this IdentityHub's DID", required = true)
    public static final String OWN_DID_PROPERTY = "edc.ih.iam.id";
    public static final String PRESENTATION_EXCHANGE_V_1_JSON = "presentation-exchange.v1.json";
    public static final String PRESENTATION_QUERY_V_08_JSON = "presentation-query.v08.json";
    public static final String DID_JSON = "did.json";
    public static final String JWS_2020_JSON = "jws2020.json";
    public static final String CREDENTIALS_V_1_JSON = "credentials.v1.json";
    private final String defaultSuite = IdentityHubConstants.JWS_2020_SIGNATURE_SUITE;
    private PresentationCreatorRegistryImpl presentationCreatorRegistry;
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
    @Inject
    private PrivateKeyResolver privateKeyResolver;
    @Inject
    private Clock clock;
    @Inject
    private SignatureSuiteRegistry signatureSuiteRegistry;

    @Override
    public String name() {
        return NAME;
    }

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

    @Provider
    public PresentationCreatorRegistry presentationCreatorRegistry(ServiceExtensionContext context) {
        if (presentationCreatorRegistry == null) {
            presentationCreatorRegistry = new PresentationCreatorRegistryImpl();
            presentationCreatorRegistry.addCreator(new JwtPresentationCreator(privateKeyResolver, clock, getOwnDid(context)), CredentialFormat.JWT);

            var ldpIssuer = LdpIssuer.Builder.newInstance().jsonLd(jsonLd).monitor(context.getMonitor()).build();
            presentationCreatorRegistry.addCreator(new LdpPresentationCreator(privateKeyResolver, getOwnDid(context), signatureSuiteRegistry, defaultSuite, ldpIssuer, null),
                    CredentialFormat.JSON_LD);
        }
        return presentationCreatorRegistry;
    }

    @Provider
    public PresentationGenerator presentationGenerator(ServiceExtensionContext context) {
        return new PresentationGeneratorImpl(CredentialFormat.JSON_LD, presentationCreatorRegistry, context.getMonitor());
    }


    private String getOwnDid(ServiceExtensionContext context) {
        return context.getConfig().getString(OWN_DID_PROPERTY);
    }

    private void cacheContextDocuments(ClassLoader classLoader) {
        try {
            jsonLd.registerCachedDocument(PRESENTATION_EXCHANGE_URL, classLoader.getResource(PRESENTATION_EXCHANGE_V_1_JSON).toURI());
            jsonLd.registerCachedDocument(IATP_CONTEXT_URL, classLoader.getResource(PRESENTATION_QUERY_V_08_JSON).toURI());
            jsonLd.registerCachedDocument(DID_CONTEXT_URL, classLoader.getResource(DID_JSON).toURI());
            jsonLd.registerCachedDocument(JWS_2020_URL, classLoader.getResource(JWS_2020_JSON).toURI());
            jsonLd.registerCachedDocument(W3C_CREDENTIALS_URL, classLoader.getResource(CREDENTIALS_V_1_JSON).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
