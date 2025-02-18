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

import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerService;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpHolderTokenVerifier;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationPipeline;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleDefinitionEvaluator;
import org.eclipse.edc.issuerservice.spi.participant.store.ParticipantStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerEqualsSubjectRule;

import java.time.Clock;

@Extension("DCP Issuer Core Extension")
public class DcpIssuerCoreExtension implements ServiceExtension {

    public static final String DCP_ISSUER_SELF_ISSUED_TOKEN_CONTEXT = "dcp-issuer-si";

    @Inject
    private TokenValidationRulesRegistry rulesRegistry;

    @Inject
    private TokenValidationService tokenValidationService;

    @Inject
    private ParticipantStore participantStore;

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


    @Override
    public void initialize(ServiceExtensionContext context) {
        rulesRegistry.addRule(DCP_ISSUER_SELF_ISSUED_TOKEN_CONTEXT, new IssuerEqualsSubjectRule());
        rulesRegistry.addRule(DCP_ISSUER_SELF_ISSUED_TOKEN_CONTEXT, new ExpirationIssuedAtValidationRule(clock, 5, false));
    }

    @Provider
    public DcpIssuerService createIssuerService() {
        return new DcpIssuerServiceImpl(transactionContext, credentialDefinitionService, issuanceProcessStore, attestationPipeline, credentialRuleDefinitionEvaluator);
    }

    @Provider
    public DcpHolderTokenVerifier createTokenVerifier() {
        return new DcpHolderTokenVerifierImpl(rulesRegistry, tokenValidationService, didPublicKeyResolver, participantStore);
    }
}
