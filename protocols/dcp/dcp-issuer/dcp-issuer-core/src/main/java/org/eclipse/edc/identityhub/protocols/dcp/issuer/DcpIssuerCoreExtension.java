/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.protocols.dcp.issuer;

import okhttp3.OkHttpClient;
import org.eclipse.edc.iam.decentralizedclaims.spi.CredentialServiceUrlResolver;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerMetadataService;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerProfile;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerService;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpHolderTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpProfileRegistry;
import org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService;
import org.eclipse.edc.identityhub.statemachine.IdentityHubStateMachineConfiguration;
import org.eclipse.edc.identityhub.verification.RequiredJtiValidationRule;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStore;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationPipeline;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.delivery.CredentialStorageClient;
import org.eclipse.edc.issuerservice.spi.issuance.events.IssuanceObservable;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleDefinitionEvaluator;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.rules.NotBeforeValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerEqualsSubjectRule;

import java.time.Clock;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialProfile.DCP_PROFILE_VC20;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

@Extension("DCP Issuer Core Extension")
public class DcpIssuerCoreExtension implements ServiceExtension {

    public static final String DCP_ISSUER_SELF_ISSUED_TOKEN_CONTEXT = "dcp-issuer-si";
    static final String ACCESSTOKEN_JTI_VALIDATION_ACTIVATE = "edc.iam.accesstoken.jti.validation";
    static final String ISSUER_PROFILE = "edc.issuer.dcp.profile";
    private static final String CREDENTIAL_SERVICE_TYPE = "CredentialService";

    @Inject
    private TokenValidationRulesRegistry rulesRegistry;

    @Inject
    private TokenValidationService tokenValidationService;

    @Inject
    private ParticipantContextStore participantContextStore;

    @Inject
    private HolderStore holderStore;

    @Inject
    private DidPublicKeyResolver didPublicKeyResolver;

    @Inject
    private CredentialDefinitionService credentialDefinitionService;

    @Inject
    private IssuanceProcessStore issuanceProcessStore;

    @Inject
    private AttestationPipeline attestationPipeline;

    @Inject
    private CredentialRuleDefinitionEvaluator credentialRuleDefinitionEvaluator;

    @Inject
    private TransactionContext transactionContext;

    @Inject
    private Clock clock;

    @Inject
    private OkHttpClient okHttpClient;

    // the credentials are delivered by the state machine of the issuance processes, while it holds their lease
    @Configuration(context = "edc.issuer.issuance")
    private IdentityHubStateMachineConfiguration stateMachineConfiguration;

    @Inject
    private ParticipantSecureTokenService secureTokenService;

    @Inject
    private Vault vault;
    @Inject
    private Monitor monitor;

    @Inject
    private TypeManager typeManager;

    @Inject
    private DidResolverRegistry didResolverRegistry;

    @Inject
    private DcpProfileRegistry profileRegistry;
    @Inject
    private CredentialGeneratorRegistry credentialGeneratorRegistry;
    @Setting(description = "The DCP profile of the issuer, e.g. 'vc20-bssl/jwt' (VCDM 2.0 Bitstring Status List) or 'vc11-sl2021/jwt' " +
            "(VCDM 1.1 StatusList2021). It determines the kind of status list that holds the status of the issued credentials, whatever their format.",
            defaultValue = DCP_PROFILE_VC20, key = ISSUER_PROFILE)
    private String profileName;
    @Setting(description = "Activates the JTI check: access tokens can only be used once to guard against replay attacks", defaultValue = "false", key = ACCESSTOKEN_JTI_VALIDATION_ACTIVATE)
    private boolean activateJtiCheck;
    @Inject(required = false)
    private JtiValidationStore jtiValidationStore;
    @Setting(description = "Allow anonymous onboarding", defaultValue = "false", key = "edc.issuance.anonymous.allowed")
    private boolean allowAnonymousCredentialRequest;
    @Inject
    private Telemetry telemetry;
    @Inject
    private IssuanceObservable issuanceObservable;

    @Override
    public void initialize(ServiceExtensionContext context) {
        rulesRegistry.addRule(DCP_ISSUER_SELF_ISSUED_TOKEN_CONTEXT, new IssuerEqualsSubjectRule());
        rulesRegistry.addRule(DCP_ISSUER_SELF_ISSUED_TOKEN_CONTEXT, new NotBeforeValidationRule(clock, 5, true));
        rulesRegistry.addRule(DCP_ISSUER_SELF_ISSUED_TOKEN_CONTEXT, new ExpirationIssuedAtValidationRule(clock, 5, false));

        if (activateJtiCheck) {
            if (jtiValidationStore == null) {
                throw new EdcException("JTI validation is activated ('%s') but no JtiValidationStore is provided.".formatted(ACCESSTOKEN_JTI_VALIDATION_ACTIVATE));
            }
            rulesRegistry.addRule(DCP_ISSUER_SELF_ISSUED_TOKEN_CONTEXT, new RequiredJtiValidationRule(jtiValidationStore, context.getMonitor()));
        }

    }

    /**
     * Rejects a profile that is not registered, or whose format cannot be generated, so that the runtime does not start
     * with it. Profiles can be registered by other extensions until now. Whether its status list type can be handled is
     * up to the status list manager.
     */
    @Override
    public void prepare() {
        var profile = profileRegistry.getProfile(profileName);
        if (profile == null) {
            throw new EdcException(unknownProfile());
        }
        if (!credentialGeneratorRegistry.supports(profile.format())) {
            throw new EdcException("The DCP profile '%s' ('%s') cannot be handled, there is no credential generator for its format %s"
                    .formatted(profileName, ISSUER_PROFILE, profile.format()));
        }
    }

    @Provider
    public DcpIssuerProfile issuerProfile() {
        return () -> ofNullable(profileRegistry.getProfile(profileName)).orElseThrow(() -> new EdcException(unknownProfile()));
    }

    @Provider
    public DcpIssuerService createIssuerService() {
        return new DcpIssuerServiceImpl(transactionContext, credentialDefinitionService, issuanceProcessStore, attestationPipeline, credentialRuleDefinitionEvaluator, profileRegistry, telemetry, issuanceObservable, vault);
    }

    @Provider
    public DcpHolderTokenVerifier createTokenVerifier() {
        return new DcpHolderTokenVerifierImpl(rulesRegistry, tokenValidationService, didPublicKeyResolver, holderStore, allowAnonymousCredentialRequest);
    }

    @Provider
    public CredentialStorageClient createCredentialStorageClient() {
        return new DcpCredentialStorageClient(stateMachineConfiguration.createNoRetryClient(okHttpClient, monitor), participantContextStore, holderStore, credentialServiceUrlResolver(), secureTokenService, vault, monitor, typeManager, JSON_LD);
    }

    @Provider
    public DcpIssuerMetadataService createIssuerMetadataService() {
        return new DcpIssuerMetadataServiceImpl(credentialDefinitionService, profileRegistry);
    }

    // TODO: refactor to use upstream DidCredentialServiceUrlResolver
    @Provider
    public CredentialServiceUrlResolver credentialServiceUrlResolver() {
        return (issuer) -> {
            var didDocument = didResolverRegistry.resolve(issuer);
            if (didDocument.failed()) {
                return didDocument.mapFailure();
            }
            return didDocument.getContent().getService().stream()
                    .filter(s -> s.getType().equals(CREDENTIAL_SERVICE_TYPE))
                    .findFirst()
                    .map(service -> success(service.getServiceEndpoint()))
                    .orElseGet(() -> failure("No Service endpoint '%s' found on DID Document.".formatted(CREDENTIAL_SERVICE_TYPE)));
        };
    }

    private String unknownProfile() {
        return "The DCP profile '%s' ('%s') is not registered".formatted(profileName, ISSUER_PROFILE);
    }
}
