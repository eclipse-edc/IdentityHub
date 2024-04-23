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

import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.identitytrust.spi.verification.SignatureSuiteRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.core.creators.JwtPresentationGenerator;
import org.eclipse.edc.identityhub.core.creators.LdpPresentationGenerator;
import org.eclipse.edc.identityhub.spi.KeyPairService;
import org.eclipse.edc.identityhub.spi.ScopeToCriterionTransformer;
import org.eclipse.edc.identityhub.spi.generator.PresentationCreatorRegistry;
import org.eclipse.edc.identityhub.spi.generator.VerifiablePresentationService;
import org.eclipse.edc.identityhub.spi.model.IdentityHubConstants;
import org.eclipse.edc.identityhub.spi.resolution.CredentialQueryResolver;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.verification.AccessTokenVerifier;
import org.eclipse.edc.identityhub.token.verification.AccessTokenVerifierImpl;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.security.signature.jws2020.Jws2020SignatureSuite;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.verifiablecredentials.linkeddata.LdpIssuer;

import java.net.URISyntaxException;
import java.time.Clock;

import static org.eclipse.edc.iam.identitytrust.spi.IatpConstants.IATP_CONTEXT_URL;
import static org.eclipse.edc.identityhub.core.CoreServicesExtension.NAME;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.DID_CONTEXT_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.JWS_2020_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.PRESENTATION_EXCHANGE_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.PRESENTATION_SUBMISSION_URL;
import static org.eclipse.edc.identityhub.spi.model.IdentityHubConstants.W3C_CREDENTIALS_URL;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * This extension provides core services for the IdentityHub that are not intended to be user-replaceable.
 */
@Extension(value = NAME)
public class CoreServicesExtension implements ServiceExtension {

    public static final String NAME = "IdentityHub Core Services Extension";
    @Setting(value = "Configure this IdentityHub's DID", required = true)
    public static final String OWN_DID_PROPERTY = "edc.ih.iam.id";

    @Setting(value = "Key alias, which was used to store the public key in the vaule", required = true)
    public static final String PUBLIC_KEY_VAULT_ALIAS_PROPERTY = "edc.ih.iam.publickey.alias";

    @Setting(value = "Path to a file that holds the public key, e.g. a PEM file. Do not use in production!")
    public static final String PUBLIC_KEY_PATH_PROPERTY = "edc.ih.iam.publickey.path";

    @Setting(value = "Public key in PEM format")
    public static final String PUBLIC_KEY_PEM = "edc.ih.iam.publickey.pem";
    public static final String PRESENTATION_EXCHANGE_V_1_JSON = "presentation-exchange.v1.json";
    public static final String PRESENTATION_QUERY_V_08_JSON = "iatp.v08.json";
    public static final String PRESENTATION_SUBMISSION_V1_JSON = "presentation-submission.v1.json";
    public static final String DID_JSON = "did.json";
    public static final String JWS_2020_JSON = "jws2020.json";
    public static final String CREDENTIALS_V_1_JSON = "credentials.v1.json";
    private final String defaultSuite = IdentityHubConstants.JWS_2020_SIGNATURE_SUITE;
    private PresentationCreatorRegistryImpl presentationCreatorRegistry;

    @Inject
    private DidPublicKeyResolver publicKeyResolver;
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
    @Inject
    private TypeManager typeManager;
    @Inject
    private TokenValidationService tokenValidationService;
    @Inject
    private TokenValidationRulesRegistry tokenValidationRulesRegistry;
    @Inject
    private Vault vault;
    @Inject
    private KeyParserRegistry keyParserRegistry;
    @Inject
    private SignatureSuiteRegistry suiteRegistry;
    @Inject
    private KeyPairService keyPairService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        // Setup API
        cacheContextDocuments(getClass().getClassLoader());
        suiteRegistry.register(IdentityHubConstants.JWS_2020_SIGNATURE_SUITE, new Jws2020SignatureSuite(JacksonJsonLd.createObjectMapper()));

    }

    @Provider
    public AccessTokenVerifier createAccessTokenVerifier(ServiceExtensionContext context) {
        return new AccessTokenVerifierImpl(tokenValidationService, createPublicKey(context), tokenValidationRulesRegistry, context.getMonitor(), publicKeyResolver);
    }


    @Provider
    public CredentialQueryResolver createCredentialQueryResolver() {
        return new CredentialQueryResolverImpl(credentialStore, transformer);
    }

    @Provider
    public PresentationCreatorRegistry presentationCreatorRegistry(ServiceExtensionContext context) {
        if (presentationCreatorRegistry == null) {
            presentationCreatorRegistry = new PresentationCreatorRegistryImpl(keyPairService);
            presentationCreatorRegistry.addCreator(new JwtPresentationGenerator(privateKeyResolver, clock, getOwnDid(context), new JwtGenerationService()), CredentialFormat.JWT);

            var ldpIssuer = LdpIssuer.Builder.newInstance().jsonLd(jsonLd).monitor(context.getMonitor()).build();
            presentationCreatorRegistry.addCreator(new LdpPresentationGenerator(privateKeyResolver, getOwnDid(context), signatureSuiteRegistry, defaultSuite, ldpIssuer, typeManager.getMapper(JSON_LD)),
                    CredentialFormat.JSON_LD);
        }
        return presentationCreatorRegistry;
    }


    @Provider
    public VerifiablePresentationService presentationGenerator(ServiceExtensionContext context) {
        return new VerifiablePresentationServiceImpl(CredentialFormat.JSON_LD, presentationCreatorRegistry(context), context.getMonitor());
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
            jsonLd.registerCachedDocument(PRESENTATION_SUBMISSION_URL, classLoader.getResource(PRESENTATION_SUBMISSION_V1_JSON).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private LocalPublicKeySupplier createPublicKey(ServiceExtensionContext context) {
        return LocalPublicKeySupplier.Builder.newInstance()
                .vault(vault)
                .vaultAlias(context.getSetting(PUBLIC_KEY_VAULT_ALIAS_PROPERTY, null))
                .publicKeyPath(context.getSetting(PUBLIC_KEY_PATH_PROPERTY, null))
                .rawString(context.getSetting(PUBLIC_KEY_PEM, null))
                .keyParserRegistry(keyParserRegistry)
                .build();
    }
}
