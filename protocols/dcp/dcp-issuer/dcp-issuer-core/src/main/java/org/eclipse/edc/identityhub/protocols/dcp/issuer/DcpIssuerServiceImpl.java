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

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerService;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpProfileRegistry;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestSpecifier;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpRequestContext;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationPipeline;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.events.IssuanceObservable;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleDefinitionEvaluator;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

public class DcpIssuerServiceImpl implements DcpIssuerService {

    private final TransactionContext transactionContext;
    private final CredentialDefinitionService credentialDefinitionService;
    private final IssuanceProcessStore issuanceProcessStore;
    private final AttestationPipeline attestationPipeline;
    private final CredentialRuleDefinitionEvaluator credentialRuleDefinitionEvaluator;
    private final DcpProfileRegistry profileRegistry;
    private final Telemetry telemetry;
    private final IssuanceObservable observable;
    private final Vault vault;

    public DcpIssuerServiceImpl(TransactionContext transactionContext,
                                CredentialDefinitionService credentialDefinitionService,
                                IssuanceProcessStore issuanceProcessStore,
                                AttestationPipeline attestationPipeline,
                                CredentialRuleDefinitionEvaluator credentialRuleDefinitionEvaluator,
                                DcpProfileRegistry profileRegistry, Telemetry telemetry, IssuanceObservable observable, Vault vault) {
        this.transactionContext = transactionContext;
        this.credentialDefinitionService = credentialDefinitionService;
        this.issuanceProcessStore = issuanceProcessStore;
        this.attestationPipeline = attestationPipeline;
        this.credentialRuleDefinitionEvaluator = credentialRuleDefinitionEvaluator;
        this.profileRegistry = profileRegistry;
        this.telemetry = telemetry;
        this.observable = observable;
        this.vault = vault;
    }

    @WithSpan(value = "issuance.initiate")
    @Override
    public ServiceResult<CredentialRequestMessage.Response> initiateCredentialsIssuance(String participantContextId, CredentialRequestMessage message, DcpRequestContext context) {
        if (message.getCredentials().isEmpty()) {
            observable.invokeForEach(l -> l.rejected(message.getHolderPid(), participantContextId, "No credentials requested"));
            return ServiceResult.badRequest("No credentials requested");
        }
        var credentialFormats = parseCredentialFormats(participantContextId, message);

        if (credentialFormats.failed()) {
            observable.invokeForEach(l -> l.rejected(message.getHolderPid(), participantContextId, credentialFormats.getFailureDetail()));
            return ServiceResult.badRequest(credentialFormats.getFailureMessages());
        }

        observable.invokeForEach(l -> l.received(message.getHolderPid(), participantContextId, credentialFormats.getContent()));
        return transactionContext.execute(() -> getCredentialsDefinitions(participantContextId, message, credentialFormats.getContent())
                .compose(credentialDefinitions -> evaluateAttestations(context, credentialDefinitions))
                .compose(this::evaluateRules)
                .compose(evaluation -> createIssuanceProcess(participantContextId, message.getHolderPid(), credentialFormats.getContent(), context, evaluation))
                .onSuccess(issuance -> {
                    // a request that was received before neither starts nor rejects anything: its process carries on
                    if (!issuance.alreadyReceived()) {
                        observable.invokeForEach(l -> l.requested(issuance.process()));
                    }
                })
                .onFailure(f -> {
                    observable.invokeForEach(l -> l.rejected(message.getHolderPid(), participantContextId, f.getFailureDetail()));
                })
                .map(issuance -> new CredentialRequestMessage.Response(issuance.process().getId(), issuance.alreadyReceived())));

    }


    private ServiceResult<Collection<CredentialDefinition>> getCredentialsDefinitions(String participantContextId, CredentialRequestMessage message, Map<String, CredentialFormat> credentialFormats) {

        var ids = message.getCredentials().stream()
                .map(CredentialRequestSpecifier::credentialObjectId)
                .collect(Collectors.toSet());

        return queryCredentialDefinitions(participantContextId, ids)
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

        var attestationIds = credentialDefinitions.stream()
                .flatMap(credentialDefinition -> credentialDefinition.getAttestations().stream())
                .collect(Collectors.toSet());

        if (attestationIds.isEmpty()) {
            return ServiceResult.badRequest("No attestations found for requested credentials");
        }

        var result = attestationPipeline.evaluate(attestationIds, new DcpAttestationContext(context));
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

    private ServiceResult<Issuance> createIssuanceProcess(String participantContextId, String holderPid, Map<String, CredentialFormat> credentialFormats, DcpRequestContext context, AttestationEvaluationResponse evaluationResponse) {

        var existing = findExisting(participantContextId, holderPid, context);
        if (existing != null) {
            return existing;
        }

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
                .traceContext(telemetry.getCurrentTraceContext())
                .credentialFormats(credentialFormats)
                .build();

        // the access token is a bearer credential, so it is kept in the vault rather than alongside the process.
        // It is presented back to the Holder's Credential Service when the credentials are delivered. It is stored first:
        // a process without it could not deliver its credentials, and would reject the Holder's retry as a duplicate.
        var hasAccessToken = context.accessToken() != null && !context.accessToken().isBlank();
        if (hasAccessToken) {
            var storeResult = vault.storeSecret(issuanceProcess.getId(), context.accessToken());
            if (storeResult.failed()) {
                return ServiceResult.unexpected("Failed to store the access token of the credential request with holderPid '%s': %s"
                        .formatted(holderPid, storeResult.getFailureDetail()));
            }
        }

        // the store rejects a second process with the same holderPid, e.g. of a request that is handled concurrently
        var saveResult = issuanceProcessStore.save(issuanceProcess);
        if (saveResult.failed()) {
            if (hasAccessToken) {
                // a token that cannot be deleted is never used, because its alias is the ID of a process that does not exist
                vault.deleteSecret(issuanceProcess.getId());
            }
            return ofNullable(findExisting(participantContextId, holderPid, context))
                    .orElseGet(() -> ServiceResult.fromFailure(saveResult));
        }

        return ServiceResult.success(new Issuance(issuanceProcess, false));
    }

    /**
     * Looks for the issuance process of a request that was received before, e.g. because the Holder sent it again after it
     * was interrupted.
     *
     * @return the existing process, if it belongs to the requesting Holder, a conflict if it belongs to another Holder, so
     *         that its ID is not disclosed, or null if there is none
     */
    private @Nullable ServiceResult<Issuance> findExisting(String participantContextId, String holderPid, DcpRequestContext context) {
        var query = QuerySpec.Builder.newInstance()
                .filter(Criterion.criterion("holderPid", "=", holderPid))
                .filter(Criterion.criterion("participantContextId", "=", participantContextId))
                .build();

        return issuanceProcessStore.query(query).findAny()
                .map(existing -> existing.getHolderId().equals(context.holder().getHolderId())
                        ? ServiceResult.success(new Issuance(existing, true))
                        : ServiceResult.<Issuance>conflict("An issuance process with holderPid '%s' already exists for this participant.".formatted(holderPid)))
                .orElse(null);

    }

    /**
     * Only the credential definitions of the participant context that the request was sent to can be requested. Those of
     * other participant contexts are treated as if they did not exist.
     */
    private ServiceResult<Collection<CredentialDefinition>> queryCredentialDefinitions(String participantContextId, Set<String> ids) {
        var query = QuerySpec.Builder.newInstance()
                .filter(Criterion.criterion("id", "in", ids))
                .filter(Criterion.criterion("participantContextId", "=", participantContextId))
                .build();
        return credentialDefinitionService.queryCredentialDefinitions(query);
    }

    private ServiceResult<Map<String, CredentialFormat>> parseCredentialFormats(String participantContextId, CredentialRequestMessage message) {
        var ids = message.getCredentials().stream()
                .map(CredentialRequestSpecifier::credentialObjectId)
                .collect(Collectors.toSet());
        var credentialDefinitions = queryCredentialDefinitions(participantContextId, ids);
        if (credentialDefinitions.failed()) {
            return credentialDefinitions.mapFailure();
        }
        var definitionsById = credentialDefinitions.getContent().stream()
                .collect(Collectors.toMap(CredentialDefinition::getId, Function.identity()));

        var credentialFormats = new HashMap<String, CredentialFormat>();
        for (var credential : message.getCredentials()) {
            try {
                var id = credential.credentialObjectId();
                var credentialDefinition = definitionsById.get(id);
                if (credentialDefinition == null) {
                    return ServiceResult.notFound("A Credential definition ID '%s' does not exist.".formatted(id));
                }
                credentialFormats.put(id, credentialDefinition.getFormatAsEnum());
            } catch (IllegalArgumentException e) {
                return ServiceResult.badRequest("Credential format not supported for credential object ID: %s".formatted(credential.credentialObjectId()));
            }
        }
        return ServiceResult.success(credentialFormats);
    }

    private record AttestationEvaluationResponse(Collection<CredentialDefinition> credentialDefinitions,
                                                 Map<String, Object> claims) {
    }

    /**
     * The issuance process of a credential request.
     *
     * @param alreadyReceived whether the process was created for an earlier copy of the request
     */
    private record Issuance(IssuanceProcess process, boolean alreadyReceived) {
    }
}
