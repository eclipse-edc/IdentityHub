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

import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerService;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequest;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpRequestContext;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationPipeline;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuerCredentialIssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuerCredentialIssuanceProcessStates;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleDefinitionEvaluator;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class DcpIssuerServiceImpl implements DcpIssuerService {

    private final TransactionContext transactionContext;
    private final CredentialDefinitionService credentialDefinitionService;
    private final IssuanceProcessStore issuanceProcessStore;
    private final AttestationPipeline attestationPipeline;
    private final CredentialRuleDefinitionEvaluator credentialRuleDefinitionEvaluator;


    public DcpIssuerServiceImpl(TransactionContext transactionContext,
                                CredentialDefinitionService credentialDefinitionService,
                                IssuanceProcessStore issuanceProcessStore,
                                AttestationPipeline attestationPipeline,
                                CredentialRuleDefinitionEvaluator credentialRuleDefinitionEvaluator) {
        this.transactionContext = transactionContext;
        this.credentialDefinitionService = credentialDefinitionService;
        this.issuanceProcessStore = issuanceProcessStore;
        this.attestationPipeline = attestationPipeline;
        this.credentialRuleDefinitionEvaluator = credentialRuleDefinitionEvaluator;
    }

    @Override
    public ServiceResult<CredentialRequestMessage.Response> initiateCredentialsIssuance(CredentialRequestMessage message, DcpRequestContext context) {
        if (message.getCredentials().isEmpty()) {
            return ServiceResult.badRequest("No credentials requested");
        }
        return transactionContext.execute(() -> getCredentialsDefinitions(message)
                .compose(credentialDefinitions -> evaluateAttestations(context, credentialDefinitions))
                .compose(this::evaluateRules)
                .compose(evaluation -> createIssuanceProcess(context, evaluation))
                .map(issuanceProcess -> new CredentialRequestMessage.Response(issuanceProcess.getId())));

    }


    private ServiceResult<Collection<CredentialDefinition>> getCredentialsDefinitions(CredentialRequestMessage message) {

        var credentialTypes = message.getCredentials().stream()
                .map(CredentialRequest::credentialType)
                .collect(Collectors.toSet());

        var query = QuerySpec.Builder.newInstance()
                .filter(Criterion.criterion("credentialType", "in", credentialTypes))
                .build();

        return credentialDefinitionService.queryCredentialDefinitions(query)
                .compose(credentialDefinitions -> validateCredentialDefinitions(message, credentialDefinitions));
    }

    private ServiceResult<Collection<CredentialDefinition>> validateCredentialDefinitions(CredentialRequestMessage message, Collection<CredentialDefinition> credentialDefinitions) {
        if (message.getCredentials().size() != credentialDefinitions.size()) {
            return ServiceResult.badRequest("Not all requested credential types have a corresponding credential definition");
        }
        return ServiceResult.success(credentialDefinitions);
    }

    private ServiceResult<AttestationEvaluationResponse> evaluateAttestations(DcpRequestContext context, Collection<CredentialDefinition> credentialDefinitions) {

        var attestations = credentialDefinitions.stream()
                .flatMap(credentialDefinition -> credentialDefinition.getAttestations().stream())
                .collect(Collectors.toSet());

        if (attestations.isEmpty()) {
            return ServiceResult.badRequest("No attestations found for requested credentials");
        }

        var result = attestationPipeline.evaluate(attestations, new DcpAttestationContext(context));
        if (result.failed()) {
            return ServiceResult.unauthorized("unauthorized");
        }
        return ServiceResult.success(new AttestationEvaluationResponse(credentialDefinitions, result.getContent()));
    }

    private ServiceResult<AttestationEvaluationResponse> evaluateRules(AttestationEvaluationResponse evaluationResponse) {

        var credentialRuleDefinitions = evaluationResponse.credentialDefinitions().stream()
                .flatMap(credentialDefinition -> credentialDefinition.getRules().stream())
                .collect(Collectors.toList());

        var result = credentialRuleDefinitionEvaluator.evaluate(credentialRuleDefinitions, evaluationResponse::claims);
        if (result.failed()) {
            return ServiceResult.unauthorized("unauthorized");
        }
        return ServiceResult.success(evaluationResponse);
    }

    private ServiceResult<IssuerCredentialIssuanceProcess> createIssuanceProcess(DcpRequestContext context, AttestationEvaluationResponse evaluationResponse) {

        var credentialDefinitionIds = evaluationResponse.credentialDefinitions().stream()
                .map(CredentialDefinition::getId)
                .collect(Collectors.toSet());

        var issuanceProcess = IssuerCredentialIssuanceProcess.Builder.newInstance()
                .participantId(context.participant().participantId())
                .state(IssuerCredentialIssuanceProcessStates.APPROVED.code())
                .credentialDefinitions(credentialDefinitionIds)
                .claims(evaluationResponse.claims())
                .build();

        issuanceProcessStore.save(issuanceProcess);
        return ServiceResult.success(issuanceProcess);
    }

    private record AttestationEvaluationResponse(Collection<CredentialDefinition> credentialDefinitions,
                                                 Map<String, Object> claims) {
    }
}
