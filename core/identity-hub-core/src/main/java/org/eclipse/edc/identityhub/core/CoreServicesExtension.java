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

import okhttp3.OkHttpClient;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.SignatureSuiteRegistry;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.identityhub.core.services.participant.HolderDataCleanup;
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
import org.eclipse.edc.identityhub.http.IssuerEndpoints;
import org.eclipse.edc.identityhub.publickey.KeyPairResourcePublicKeyResolver;
import org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.model.IdentityHubConstants;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextDeleted;
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
import org.eclipse.edc.identityhub.spi.verifiablecredentials.validation.CredentialFormatValidator;
import org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenVerifier;
import org.eclipse.edc.identityhub.statemachine.IdentityHubStateMachineConfiguration;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.jwt.spi.signer.JwsSignerProvider;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.keys.spi.LocalPublicKeyService;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.security.signature.jws2020.Jws2020SignatureSuite;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
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
import java.time.Duration;

import static org.eclipse.edc.identityhub.core.CoreServicesExtension.NAME;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_1_0;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * This extension provides core services for the IdentityHub that are not intended to be user-replaceable.
 */
@Extension(value = NAME)
public class CoreServicesExtension implements ServiceExtension {

    public static final String NAME = "IdentityHub Core Services Extension";
    public static final long DEFAULT_PRESENTATION_VALIDITY_SECONDS = 60;
    public static final String ACCESS_TOKEN_KEY_FALLBACK_SETTING = "edc.iam.accesstoken.publickey.fallback.enabled";
    public static final String REJECT_UNVERIFIED_CREDENTIALS_SETTING = "edc.iam.credential.delivery.reject.unverified";
    public static final String PLAIN_VCDM20_PRESENTATION_SETTING = "edc.iam.presentation.vcdm20.plain.enabled";

    @Setting(description = "How long a Verifiable Presentation is valid after it was created, in seconds. Determines the 'exp' claim of JWT presentations " +
            "and the proof 'expires' of Linked-Data presentations.",
            defaultValue = DEFAULT_PRESENTATION_VALIDITY_SECONDS + "", key = "edc.iam.presentation.validity")
    private long presentationValiditySeconds;

    @Setting(description = "Returns VCDM 2.0 presentations as the VP JWT itself, as defined by VC-JOSE-COSE, instead of wrapping it in an 'EnvelopedVerifiablePresentation' " +
            "that is signed again as a JWT. Only enable this if all verifiers accept plain VP JWTs.",
            defaultValue = "false", key = PLAIN_VCDM20_PRESENTATION_SETTING)
    private boolean plainVcdm20Presentation;

    @Setting(description = "Verifies access tokens with keys from the vault or 'edc.iam.publickeys' if the participant context has no matching key pair. " +
            "That lookup is not scoped to the participant context, so this should only be enabled for keys that are not managed as key pairs.",
            defaultValue = "false", key = ACCESS_TOKEN_KEY_FALLBACK_SETTING)
    private boolean accessTokenKeyFallback;

    @Setting(description = "Rejects delivered credentials in formats whose proof cannot be verified, such as credentials with an embedded Linked-Data proof. " +
            "If disabled, they are stored with a warning.",
            defaultValue = "false", key = REJECT_UNVERIFIED_CREDENTIALS_SETTING)
    private boolean rejectUnverifiedCredentials;

    @Setting(description = IssuerEndpoints.RESTRICT_DESCRIPTION, key = IssuerEndpoints.RESTRICT_SETTING, defaultValue = "false")
    private boolean restrictIssuerEndpoints;

    @Setting(description = IssuerEndpoints.ALLOWED_HOSTS_DESCRIPTION, key = IssuerEndpoints.ALLOWED_HOSTS_SETTING, required = false)
    private String allowedIssuerHosts;

    @Configuration
    private CredentialRequestConfiguration credentialRequestConfiguration;

    @Configuration(context = "edc.iam.credential.request")
    private IdentityHubStateMachineConfiguration credentialRequestStateMachineConfiguration;

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
    private IdentityHubParticipantContextService participantContextService;
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
    private OkHttpClient okHttpClient;
    @Inject
    private HolderCredentialRequestStore holderCredentialRequestStore;
    @Inject
    private CredentialOfferStore credentialOfferStore;
    @Inject
    private EventRouter eventRouter;
    @Inject
    private ExecutorInstrumentation executorInstrumentation;

    private CredentialRequestManagerImpl credentialRequestService;
    private CredentialOfferObservable credentialOfferObservable;
    private PresentationCreatorRegistryImpl presentationCreatorRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        suiteRegistry.register(IdentityHubConstants.JWS_2020_SIGNATURE_SUITE, new Jws2020SignatureSuite(JacksonJsonLd.createObjectMapper()));
        eventRouter.registerSync(ParticipantContextDeleted.class,
                new HolderDataCleanup(credentialStore, holderCredentialRequestStore, credentialOfferStore, transactionContext, context.getMonitor()));
        if (accessTokenKeyFallback) {
            context.getMonitor().warning("'%s' is enabled: access tokens are also verified with keys that do not belong to a key pair of the participant context.".formatted(ACCESS_TOKEN_KEY_FALLBACK_SETTING));
        }
    }

    @Override
    public void start() {
        credentialRequestService.start();
    }

    @Override
    public void shutdown() {
        credentialRequestService.stop();
    }

    @Provider
    public SelfIssuedTokenVerifier createAccessTokenVerifier(ServiceExtensionContext context) {
        // access tokens are issued by this IdentityHub with a key pair of the participant context, so other keys are only used if enabled
        var keyResolver = new KeyPairResourcePublicKeyResolver(store, keyParserRegistry, context.getMonitor(), accessTokenKeyFallback ? fallbackService : null);
        return new SelfIssuedTokenVerifierImpl(tokenValidationService, keyResolver, tokenValidationRulesRegistry, publicKeyResolver, participantContextService, clock);
    }

    @Provider
    public CredentialQueryResolver createCredentialQueryResolver(ServiceExtensionContext context) {
        return new CredentialQueryResolverImpl(credentialStore, transformer, revocationServiceRegistry, context.getMonitor().withPrefix("Credential Query"), clock);
    }

    @Provider
    public PresentationCreatorRegistry presentationCreatorRegistry(ServiceExtensionContext context) {
        if (presentationCreatorRegistry == null) {
            presentationCreatorRegistry = new PresentationCreatorRegistryImpl(keyPairService, participantContextService, transactionContext);
            var jwtGenerationService = new JwtGenerationService(jwsSignerProvider);
            var monitor = context.getMonitor();
            var presentationValidity = presentationValidity(monitor);
            presentationCreatorRegistry.addCreator(new JwtPresentationGenerator(clock, jwtGenerationService, presentationValidity), CredentialFormat.VC1_0_JWT);

            var ldpIssuer = LdpIssuer.Builder.newInstance().jsonLd(jsonLd).monitor(monitor).build();
            presentationCreatorRegistry.addCreator(new LdpPresentationGenerator(privateKeyResolver, signatureSuiteRegistry, IdentityHubConstants.JWS_2020_SIGNATURE_SUITE, ldpIssuer, typeManager, JSON_LD, clock, presentationValidity),
                    CredentialFormat.VC1_0_LD);

            presentationCreatorRegistry.addCreator(new JwtEnvelopedPresentationGenerator(monitor, clock, jwtGenerationService, presentationValidity, plainVcdm20Presentation), CredentialFormat.VC2_0_JOSE);
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
        return new CredentialWriterImpl(credentialStore, typeTransformerRegistry, transactionContext, objectMapper, holderCredentialRequestStore, tokenValidationService, publicKeyResolver,
                new CredentialFormatValidator(), context.getMonitor(), clock, rejectUnverifiedCredentials);
    }

    @Provider
    public CredentialStatusCheckService createStatusCheckService() {
        return new CredentialStatusCheckServiceImpl(revocationServiceRegistry, clock);
    }

    @Provider
    public CredentialRequestManager createDefaultCredentialRequestService(ServiceExtensionContext context) {
        if (credentialRequestService == null) {
            credentialRequestStateMachineConfiguration.warnIfBatchMayOutlastLease(context.getMonitor(), "credential request");
            var allowedHosts = IssuerEndpoints.parseAllowedHosts(allowedIssuerHosts);
            var issuerHttpClient = restrictIssuerEndpoints ? IssuerEndpoints.restrict(okHttpClient, allowedHosts) : okHttpClient;
            credentialRequestService = CredentialRequestManagerImpl.Builder.newInstance()
                    .store(credentialRequestStore)
                    .didResolverRegistry(didResolverRegistry)
                    .typeTransformerRegistry(typeTransformerRegistry.forContext(DCP_SCOPE_V_1_0))
                    .jsonLd(jsonLd)
                    .httpClient(credentialRequestStateMachineConfiguration.createNoRetryClient(issuerHttpClient, context.getMonitor()))
                    .restrictIssuerEndpoints(restrictIssuerEndpoints)
                    .allowedIssuerHosts(allowedHosts)
                    .secureTokenService(secureTokenService)
                    .transactionContext(transactionContext)
                    .participantContextService(participantContextService)
                    .monitor(context.getMonitor())
                    .clock(clock)
                    .executorInstrumentation(executorInstrumentation)
                    .configuration(credentialRequestConfiguration)
                    .batchSize(credentialRequestStateMachineConfiguration.batchSize())
                    .waitStrategy(credentialRequestStateMachineConfiguration.iterationWaitExponentialWaitStrategy())
                    .entityRetryProcessConfiguration(credentialRequestStateMachineConfiguration.entityRetryProcessConfiguration())
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

    private Duration presentationValidity(Monitor monitor) {
        if (presentationValiditySeconds <= 0) {
            monitor.warning("'edc.iam.presentation.validity' must be positive, but was %d. Using the default of %d seconds instead."
                    .formatted(presentationValiditySeconds, DEFAULT_PRESENTATION_VALIDITY_SECONDS));
            return Duration.ofSeconds(DEFAULT_PRESENTATION_VALIDITY_SECONDS);
        }
        return Duration.ofSeconds(presentationValiditySeconds);
    }
}
