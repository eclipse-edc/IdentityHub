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
 *       Cofinity-X - Improvements for VC DataModel 2.0
 *
 */

package org.eclipse.edc.identityhub.core;

import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.identitytrust.spi.verification.SignatureSuiteRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.identityhub.core.services.query.CredentialQueryResolverImpl;
import org.eclipse.edc.identityhub.core.services.verifiablecredential.CredentialOfferEventPublisher;
import org.eclipse.edc.identityhub.core.services.verifiablecredential.CredentialOfferObservableImpl;
import org.eclipse.edc.identityhub.core.services.verifiablecredential.CredentialOfferServiceImpl;
import org.eclipse.edc.identityhub.core.services.verifiablecredential.CredentialRequestManagerImpl;
import org.eclipse.edc.identityhub.core.services.verifiablecredential.CredentialStatusCheckServiceImpl;
import org.eclipse.edc.identityhub.core.services.verifiablecredential.CredentialWriterImpl;
import org.eclipse.edc.identityhub.core.services.verifiablepresentation.PresentationCreatorRegistryImpl;
import org.eclipse.edc.identityhub.core.services.verifiablepresentation.VerifiablePresentationServiceImpl;
import org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators.JwtEnvelopedPresentationGenerator;
import org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators.JwtPresentationGenerator;
import org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators.LdpPresentationGenerator;
import org.eclipse.edc.identityhub.core.services.verification.SelfIssuedTokenVerifierImpl;
import org.eclipse.edc.identityhub.publickey.KeyPairResourcePublicKeyResolver;
import org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.model.IdentityHubConstants;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.transformation.ScopeToCriterionTransformer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestManager;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialStatusCheckService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.events.CredentialOfferObservable;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.CredentialWriter;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.PresentationCreatorRegistry;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.VerifiablePresentationService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.offer.CredentialOfferService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.resolution.CredentialQueryResolver;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenVerifier;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.jwt.signer.spi.JwsSignerProvider;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.keys.spi.LocalPublicKeyService;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.security.signature.jws2020.Jws2020SignatureSuite;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.verifiablecredentials.linkeddata.LdpIssuer;

import java.time.Clock;

import static org.eclipse.edc.identityhub.core.CoreServicesExtension.NAME;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_1_0;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * This extension provides core services for the IdentityHub that are not intended to be user-replaceable.
 */
@Extension(value = NAME)
public class CoreServicesExtension implements ServiceExtension {

    public static final String NAME = "IdentityHub Core Services Extension";

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
    @Inject
    private RevocationServiceRegistry revocationServiceRegistry;
    @Inject
    private KeyPairResourceStore store;

    @Inject
    private LocalPublicKeyService fallbackService;
    @Inject
    private ParticipantContextService participantContextService;
    @Inject
    private JwsSignerProvider jwsSignerProvider;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;

    @Inject
    private DidResolverRegistry didResolverRegistry;
    @Inject
    private HolderCredentialRequestStore credentialRequestStore;
    @Inject
    private ParticipantSecureTokenService secureTokenService;
    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private HolderCredentialRequestStore holderCredentialRequestStore;
    @Inject
    private CredentialOfferStore credentialOfferStore;
    @Inject
    private EventRouter eventRouter;
    private CredentialRequestManagerImpl credentialRequestService;
    private CredentialOfferObservable credentialOfferObservable;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        suiteRegistry.register(IdentityHubConstants.JWS_2020_SIGNATURE_SUITE, new Jws2020SignatureSuite(JacksonJsonLd.createObjectMapper()));
    }

    @Override
    public void start() {
        credentialRequestService.start();
    }

    @Provider
    public SelfIssuedTokenVerifier createAccessTokenVerifier(ServiceExtensionContext context) {
        var keyResolver = new KeyPairResourcePublicKeyResolver(store, keyParserRegistry, context.getMonitor(), fallbackService);
        return new SelfIssuedTokenVerifierImpl(tokenValidationService, keyResolver, tokenValidationRulesRegistry, publicKeyResolver, participantContextService);
    }

    @Provider
    public CredentialQueryResolver createCredentialQueryResolver(ServiceExtensionContext context) {
        return new CredentialQueryResolverImpl(credentialStore, transformer, revocationServiceRegistry, context.getMonitor().withPrefix("Credential Query"));
    }

    @Provider
    public PresentationCreatorRegistry presentationCreatorRegistry(ServiceExtensionContext context) {
        if (presentationCreatorRegistry == null) {
            presentationCreatorRegistry = new PresentationCreatorRegistryImpl(keyPairService, participantContextService, transactionContext);
            var jwtGenerationService = new JwtGenerationService(jwsSignerProvider);
            presentationCreatorRegistry.addCreator(new JwtPresentationGenerator(clock, jwtGenerationService), CredentialFormat.VC1_0_JWT);

            var monitor = context.getMonitor();
            var ldpIssuer = LdpIssuer.Builder.newInstance().jsonLd(jsonLd).monitor(monitor).build();
            presentationCreatorRegistry.addCreator(new LdpPresentationGenerator(privateKeyResolver, signatureSuiteRegistry, IdentityHubConstants.JWS_2020_SIGNATURE_SUITE, ldpIssuer, typeManager, JSON_LD),
                    CredentialFormat.VC1_0_LD);

            presentationCreatorRegistry.addCreator(new JwtEnvelopedPresentationGenerator(monitor, jwtGenerationService), CredentialFormat.VC2_0_JOSE);
        }
        return presentationCreatorRegistry;
    }

    @Provider
    public VerifiablePresentationService presentationGenerator(ServiceExtensionContext context) {
        return new VerifiablePresentationServiceImpl(presentationCreatorRegistry(context), context.getMonitor());
    }

    @Provider
    public CredentialWriter createCredentialWriter(ServiceExtensionContext context) {
        var objectMapper = typeManager.getMapper(JSON_LD);
        return new CredentialWriterImpl(credentialStore, typeTransformerRegistry, transactionContext, objectMapper, holderCredentialRequestStore);
    }

    @Provider
    public CredentialStatusCheckService createStatusCheckService() {
        return new CredentialStatusCheckServiceImpl(revocationServiceRegistry, clock);
    }

    @Provider
    public CredentialRequestManager createDefaultCredentialRequestService(ServiceExtensionContext context) {
        if (credentialRequestService == null) {
            credentialRequestService = CredentialRequestManagerImpl.Builder.newInstance()
                    .store(credentialRequestStore)
                    .didResolverRegistry(didResolverRegistry)
                    .typeTransformerRegistry(typeTransformerRegistry.forContext(DCP_SCOPE_V_1_0))
                    .httpClient(httpClient)
                    .secureTokenService(secureTokenService)
                    .transactionContext(transactionContext)
                    .participantContextService(participantContextService)
                    .monitor(context.getMonitor())
                    .build();
        }
        return credentialRequestService;
    }

    @Provider
    public CredentialOfferService createDefaultCredentialOfferService() {
        return new CredentialOfferServiceImpl(credentialOfferStore, transactionContext, credentialOfferObservable());
    }

    @Provider
    public CredentialOfferObservable credentialOfferObservable() {
        if (credentialOfferObservable == null) {
            credentialOfferObservable = new CredentialOfferObservableImpl();
            credentialOfferObservable.registerListener(new CredentialOfferEventPublisher(clock, eventRouter));
        }
        return credentialOfferObservable;
    }
}
