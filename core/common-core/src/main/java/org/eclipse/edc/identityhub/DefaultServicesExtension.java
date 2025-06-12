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

import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.identitytrust.spi.verification.SignatureSuiteRegistry;
import org.eclipse.edc.iam.verifiablecredentials.revocation.RevocationServiceRegistryImpl;
import org.eclipse.edc.iam.verifiablecredentials.revocation.bitstring.BitstringStatusListRevocationService;
import org.eclipse.edc.iam.verifiablecredentials.revocation.statuslist2021.StatusList2021RevocationService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.statuslist2021.StatusList2021Status;
import org.eclipse.edc.identityhub.accesstoken.rules.ClaimIsPresentRule;
import org.eclipse.edc.identityhub.defaults.EdcScopeToCriterionTransformer;
import org.eclipse.edc.identityhub.defaults.store.InMemoryCredentialOfferStore;
import org.eclipse.edc.identityhub.defaults.store.InMemoryCredentialStore;
import org.eclipse.edc.identityhub.defaults.store.InMemoryHolderCredentialRequestStore;
import org.eclipse.edc.identityhub.defaults.store.InMemoryKeyPairResourceStore;
import org.eclipse.edc.identityhub.defaults.store.InMemoryParticipantContextStore;
import org.eclipse.edc.identityhub.defaults.store.InMemorySignatureSuiteRegistry;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.keypair.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.store.ParticipantContextStore;
import org.eclipse.edc.identityhub.spi.transformation.ScopeToCriterionTransformer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jwt.signer.spi.JwsSignerProvider;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.security.token.jwt.DefaultJwsSignerProvider;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.rules.NotBeforeValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.verifiablecredentials.jwt.rules.JtiValidationRule;

import java.net.URISyntaxException;
import java.time.Clock;
import java.util.List;

import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DCP_CONTEXT_URL;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_V_1_0_CONTEXT;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.DID_CONTEXT_URL;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.JWS_2020_URL;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.PRESENTATION_EXCHANGE_URL;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.PRESENTATION_SUBMISSION_URL;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.W3C_CREDENTIALS_URL;
import static org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry.WILDCARD;
import static org.eclipse.edc.identityhub.DefaultServicesExtension.NAME;
import static org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenConstants.ACCESS_TOKEN_SCOPE_CLAIM;
import static org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenConstants.DCP_PRESENTATION_ACCESS_TOKEN_CONTEXT;
import static org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenConstants.DCP_PRESENTATION_SELF_ISSUED_TOKEN_CONTEXT;
import static org.eclipse.edc.identityhub.spi.verification.SelfIssuedTokenConstants.TOKEN_CLAIM;

@Extension(NAME)
public class DefaultServicesExtension implements ServiceExtension {
    public static final String PRESENTATION_EXCHANGE_V_1_JSON = "presentation-exchange.v1.json";
    public static final String PRESENTATION_QUERY_V_08_JSON = "dcp.v08.json";
    public static final String DSPACE_DCP_V_1_0_JSON_LD = "dcp.v1.0.jsonld";
    public static final String PRESENTATION_SUBMISSION_V1_JSON = "presentation-submission.v1.json";
    public static final String DID_JSON = "did.json";
    public static final String JWS_2020_JSON = "jws2020.json";
    public static final String CREDENTIALS_V_1_JSON = "credentials.v1.json";


    public static final String NAME = "IdentityHub Default Services Extension";
    public static final long DEFAULT_REVOCATION_CACHE_VALIDITY_MILLIS = 15 * 60 * 1000L;
    static final String ACCESSTOKEN_JTI_VALIDATION_ACTIVATE = "edc.iam.accesstoken.jti.validation";
    @Setting(description = "Activates the JTI check: access tokens can only be used once to guard against replay attacks", defaultValue = "false", key = ACCESSTOKEN_JTI_VALIDATION_ACTIVATE)
    private boolean activateJtiCheck;
    @Setting(description = "Validity period of cached StatusList2021 credential entries in milliseconds.", defaultValue = DEFAULT_REVOCATION_CACHE_VALIDITY_MILLIS + "", key = "edc.iam.credential.revocation.cache.validity")
    private long revocationCacheValidity;
    @Setting(key = "edc.iam.credential.revocation.mimetype", description = "A comma-separated list of accepted content types of the revocation list credential.", defaultValue = WILDCARD)
    private String contentTypes;

    @Inject
    private TokenValidationRulesRegistry registry;
    @Inject
    private TypeManager typeManager;
    private RevocationServiceRegistry revocationService;
    @Inject
    private PrivateKeyResolver privateKeyResolver;
    @Inject
    private JtiValidationStore jwtValidationStore;
    @Inject
    private Clock clock;
    @Inject
    private CriterionOperatorRegistry criterionOperatorRegistry;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private JtiValidationStore jtiValidationStore;
    @Inject
    private EdcHttpClient httpClient;

    @Override
    public String name() {
        return NAME;
    }


    @Override
    public void initialize(ServiceExtensionContext context) {
        var accessTokenRule = new ClaimIsPresentRule(TOKEN_CLAIM);
        registry.addRule(DCP_PRESENTATION_SELF_ISSUED_TOKEN_CONTEXT, accessTokenRule);
        registry.addRule(DCP_PRESENTATION_SELF_ISSUED_TOKEN_CONTEXT, new ExpirationIssuedAtValidationRule(clock, 5, true));
        registry.addRule(DCP_PRESENTATION_SELF_ISSUED_TOKEN_CONTEXT, new NotBeforeValidationRule(clock, 5, true));

        var scopeIsPresentRule = new ClaimIsPresentRule(ACCESS_TOKEN_SCOPE_CLAIM);
        registry.addRule(DCP_PRESENTATION_ACCESS_TOKEN_CONTEXT, scopeIsPresentRule);

        if (activateJtiCheck) {
            registry.addRule(DCP_PRESENTATION_SELF_ISSUED_TOKEN_CONTEXT, new JtiValidationRule(jtiValidationStore, context.getMonitor()));
            registry.addRule(DCP_PRESENTATION_ACCESS_TOKEN_CONTEXT, new JtiValidationRule(jwtValidationStore, context.getMonitor()));
        } else {
            context.getMonitor().warning("JWT Token ID (\"jti\" claim) Validation is not active. Please consider setting '%s=true' for protection against replay attacks".formatted(ACCESSTOKEN_JTI_VALIDATION_ACTIVATE));
        }

        // Setup API
        cacheContextDocuments(getClass().getClassLoader());
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
    public RevocationServiceRegistry createRevocationListService(ServiceExtensionContext context) {
        if (revocationService == null) {
            revocationService = new RevocationServiceRegistryImpl(context.getMonitor());
            var acceptedContentTypes = List.of(contentTypes.split(","));
            revocationService.addService(StatusList2021Status.TYPE, new StatusList2021RevocationService(typeManager.getMapper(), revocationCacheValidity, acceptedContentTypes, httpClient));
            revocationService.addService(BitstringStatusListStatus.TYPE, new BitstringStatusListRevocationService(typeManager.getMapper(), revocationCacheValidity, acceptedContentTypes, httpClient));
        }
        return revocationService;
    }

    @Provider(isDefault = true)
    public SignatureSuiteRegistry createSignatureSuiteRegistry() {
        return new InMemorySignatureSuiteRegistry();
    }

    @Provider(isDefault = true)
    public JwsSignerProvider defaultSignerProvider() {
        return new DefaultJwsSignerProvider(privateKeyResolver);
    }

    @Provider(isDefault = true)
    public HolderCredentialRequestStore createHolderCredentialRequestStore() {
        return new InMemoryHolderCredentialRequestStore(clock, criterionOperatorRegistry);
    }

    @Provider(isDefault = true)
    public CredentialOfferStore createCredentialOfferStore() {
        return new InMemoryCredentialOfferStore(clock, criterionOperatorRegistry);
    }

    private void cacheContextDocuments(ClassLoader classLoader) {
        try {
            jsonLd.registerCachedDocument(PRESENTATION_EXCHANGE_URL, classLoader.getResource(PRESENTATION_EXCHANGE_V_1_JSON).toURI());
            jsonLd.registerCachedDocument(DCP_CONTEXT_URL, classLoader.getResource(PRESENTATION_QUERY_V_08_JSON).toURI());
            jsonLd.registerCachedDocument(DSPACE_DCP_V_1_0_CONTEXT, classLoader.getResource(DSPACE_DCP_V_1_0_JSON_LD).toURI());
            jsonLd.registerCachedDocument(DID_CONTEXT_URL, classLoader.getResource(DID_JSON).toURI());
            jsonLd.registerCachedDocument(JWS_2020_URL, classLoader.getResource(JWS_2020_JSON).toURI());
            jsonLd.registerCachedDocument(W3C_CREDENTIALS_URL, classLoader.getResource(CREDENTIALS_V_1_JSON).toURI());
            jsonLd.registerCachedDocument(PRESENTATION_SUBMISSION_URL, classLoader.getResource(PRESENTATION_SUBMISSION_V1_JSON).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
