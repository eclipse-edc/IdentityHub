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
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpProfileRegistry;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestSpecifier;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpProfile;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpRequestContext;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationPipeline;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialRuleDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcessStates;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleDefinitionEvaluator;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DcpIssuerServiceImplTest {

    private final AttestationPipeline attestationPipeline = mock();
    private final CredentialDefinitionService credentialDefinitionService = mock();
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final IssuanceProcessStore issuanceProcessStore = mock();
    private final CredentialRuleDefinitionEvaluator credentialRuleDefinitionEvaluator = mock();
    private final DcpProfileRegistry dcpProfileRegistry = mock();

    private final DcpIssuerService dcpIssuerService = new DcpIssuerServiceImpl(transactionContext, credentialDefinitionService, issuanceProcessStore, attestationPipeline, credentialRuleDefinitionEvaluator, dcpProfileRegistry);


    @Test
    void initiateCredentialsIssuance() {

        var message = CredentialRequestMessage.Builder.newInstance()
                .holderPid(UUID.randomUUID().toString())
                .credential(new CredentialRequestSpecifier("credentialDefinitionId1"))
                .build();

        var attestations = Set.of("attestation1", "attestation2");

        var credentialRuleDefinition = new CredentialRuleDefinition("expression", Map.of());
        var credentialDefinition = CredentialDefinition.Builder.newInstance()
                .id("credentialDefinitionId1")
                .credentialType("MembershipCredential")
                .jsonSchema("jsonSchema")
                .jsonSchemaUrl("jsonSchemaUrl")
                .attestations(attestations)
                .participantContextId("participantContextId")
                .rule(credentialRuleDefinition)
                .formatFrom(VC1_0_JWT)
                .build();

        var holder = Holder.Builder.newInstance().holderId("holderId").did("participantDid").holderName("name").participantContextId("participantContextId").build();
        var participant = new DcpRequestContext(holder, Map.of());

        Map<String, Object> claims = Map.of("claim1", "value1", "claim2", "value2");

        when(credentialDefinitionService.queryCredentialDefinitions(any())).thenReturn(ServiceResult.success(List.of(credentialDefinition)));
        when(credentialDefinitionService.findCredentialDefinitionById(anyString())).thenReturn(ServiceResult.success(credentialDefinition));
        when(attestationPipeline.evaluate(eq(attestations), any())).thenReturn(Result.success(claims));
        when(credentialRuleDefinitionEvaluator.evaluate(eq(List.of(credentialRuleDefinition)), any())).thenReturn(Result.success());
        when(dcpProfileRegistry.profilesFor(VC1_0_JWT)).thenReturn(List.of(new DcpProfile("profile", VC1_0_JWT, "statusType")));

        var result = dcpIssuerService.initiateCredentialsIssuance("participantContextId", message, participant);

        assertThat(result).isSucceeded();

        var captor = ArgumentCaptor.forClass(IssuanceProcess.class);
        verify(issuanceProcessStore).save(captor.capture());

        var issuanceProcess = captor.getValue();

        assertThat(issuanceProcess).isNotNull();
        assertThat(issuanceProcess.getId()).isEqualTo(result.getContent().requestId());
        assertThat(issuanceProcess.getCredentialDefinitions()).containsExactly("credentialDefinitionId1");
        assertThat(issuanceProcess.getHolderId()).isEqualTo("holderId");
        assertThat(issuanceProcess.getState()).isEqualTo(IssuanceProcessStates.APPROVED.code());
        assertThat(issuanceProcess.getClaims()).containsAllEntriesOf(claims);
        assertThat(issuanceProcess.getParticipantContextId()).isEqualTo("participantContextId");
        assertThat(issuanceProcess.getHolderPid()).isEqualTo(message.getHolderPid());
    }

}
