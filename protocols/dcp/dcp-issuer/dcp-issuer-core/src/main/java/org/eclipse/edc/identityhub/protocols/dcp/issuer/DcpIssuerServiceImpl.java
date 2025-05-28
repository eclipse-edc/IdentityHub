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

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerService;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpProfileRegistry;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestSpecifier;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpRequestContext;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationPipeline;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleDefinitionEvaluator;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DcpIssuerServiceImpl implements DcpIssuerService {

    private final TransactionContext transactionContext;
    private final CredentialDefinitionService credentialDefinitionService;
    private final IssuanceProcessStore issuanceProcessStore;
    private final AttestationPipeline attestationPipeline;
    private final CredentialRuleDefinitionEvaluator credentialRuleDefinitionEvaluator;
    private final DcpProfileRegistry profileRegistry;

    public DcpIssuerServiceImpl(TransactionContext transactionContext,
                                CredentialDefinitionService credentialDefinitionService,
                                IssuanceProcessStore issuanceProcessStore,
                                AttestationPipeline attestationPipeline,
                                CredentialRuleDefinitionEvaluator credentialRuleDefinitionEvaluator,
                                DcpProfileRegistry profileRegistry) {
        this.transactionContext = transactionContext;
        this.credentialDefinitionService = credentialDefinitionService;
        this.issuanceProcessStore = issuanceProcessStore;
        this.attestationPipeline = attestationPipeline;
        this.credentialRuleDefinitionEvaluator = credentialRuleDefinitionEvaluator;
        this.profileRegistry = profileRegistry;
    }

    @Override
    public ServiceResult<CredentialRequestMessage.Response> initiateCredentialsIssuance(String participantContextId, CredentialRequestMessage message, DcpRequestContext context) {
        if (message.getCredentials().isEmpty()) {
            return ServiceResult.badRequest("No credentials requested");
        }
        var credentialFormats = parseCredentialFormats(message);

        if (credentialFormats.failed()) {
            return ServiceResult.badRequest(credentialFormats.getFailureMessages());

        }
        return transactionContext.execute(() -> getCredentialsDefinitions(message, credentialFormats.getContent())
                .compose(credentialDefinitions -> evaluateAttestations(context, credentialDefinitions))
                .compose(this::evaluateRules)
                .compose(evaluation -> createIssuanceProcess(participantContextId, message.getHolderPid(), credentialFormats.getContent(), context, evaluation))
                .map(issuanceProcess -> new CredentialRequestMessage.Response(issuanceProcess.getId())));

    }


    private ServiceResult<Collection<CredentialDefinition>> getCredentialsDefinitions(CredentialRequestMessage message, Map<String, CredentialFormat> credentialFormats) {

        var ids = message.getCredentials().stream()
                .map(CredentialRequestSpecifier::credentialObjectId)
                .collect(Collectors.toSet());

        var query = QuerySpec.Builder.newInstance()
                .filter(Criterion.criterion("id", "in", ids))
                .build();

        return credentialDefinitionService.queryCredentialDefinitions(query)
                .compose(credentialDefinitions -> validateCredentialDefinitions(message, credentialDefinitions, credentialFormats));
    }

    private ServiceResult<Collection<CredentialDefinition>> validateCredentialDefinitions(CredentialRequestMessage message, Collection<CredentialDefinition> credentialDefinitions, Map<String, CredentialFormat> requestedFormats) {
        if (message.getCredentials().size() != credentialDefinitions.size()) {
            return ServiceResult.badRequest("Not all requested credential types have a corresponding credential definition");
        }
        for (var credentialDefinition : credentialDefinitions) {
            var requestedFormat = requestedFormats.get(credentialDefinition.getId());
            if (!credentialDefinition.getFormatAsEnum().equals(requestedFormat)) {
                return ServiceResult.badRequest("Credential format %s not supported for credential type %s".formatted(requestedFormat, credentialDefinition.getCredentialType()));
            }
            if (profileRegistry.profilesFor(requestedFormat).isEmpty()) {
                return ServiceResult.badRequest("No DCP profiles found for credential format %s".formatted(requestedFormat));
            }
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

    private ServiceResult<IssuanceProcess> createIssuanceProcess(String participantContextId, String holderPid, Map<String, CredentialFormat> credentialFormats, DcpRequestContext context, AttestationEvaluationResponse evaluationResponse) {

        var credentialDefinitionIds = evaluationResponse.credentialDefinitions().stream()
                .map(CredentialDefinition::getId)
                .collect(Collectors.toSet());
        var issuanceProcess = IssuanceProcess.Builder.newInstance()
                .holderId(context.holder().getHolderId())
                .state(IssuanceProcessStates.APPROVED.code())
                .credentialDefinitions(credentialDefinitionIds)
                .claims(evaluationResponse.claims())
                .participantContextId(participantContextId)
                .holderPid(holderPid)
                .credentialFormats(credentialFormats)
                .build();

        issuanceProcessStore.save(issuanceProcess);
        return ServiceResult.success(issuanceProcess);

    }

    private ServiceResult<Map<String, CredentialFormat>> parseCredentialFormats(CredentialRequestMessage message) {
        var credentialFormats = new HashMap<String, CredentialFormat>();
        for (var credential : message.getCredentials()) {
            try {
                var id = credential.credentialObjectId();
                var credentialDefinition = credentialDefinitionService.findCredentialDefinitionById(id);
                if (credentialDefinition.failed()) {
                    return credentialDefinition.mapFailure();
                }
                var format = credentialDefinition.getContent().getFormatAsEnum();
                credentialFormats.put(credential.credentialObjectId(), format);
            } catch (IllegalArgumentException e) {
                return ServiceResult.badRequest("Credential format not supported for credential object ID: %s".formatted(credential.credentialObjectId()));
            }
        }
        return ServiceResult.success(credentialFormats);
    }

    private record AttestationEvaluationResponse(Collection<CredentialDefinition> credentialDefinitions,
                                                 Map<String, Object> claims) {
    }
}
