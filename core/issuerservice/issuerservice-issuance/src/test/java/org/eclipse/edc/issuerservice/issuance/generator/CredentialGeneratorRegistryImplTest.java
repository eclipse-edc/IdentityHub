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

package org.eclipse.edc.issuerservice.issuance.generator;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.issuerservice.spi.holder.HolderService;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGenerationRequest;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGenerator;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.mapping.IssuanceClaimsMapper;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.MappingDefinition;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CredentialGeneratorRegistryImplTest {


    private final IssuanceClaimsMapper claimsMapper = mock();
    private final ParticipantContextService participantContextService = mock();
    private final HolderService holderService = mock();
    private final KeyPairService keyPairService = mock();
    private final CredentialGeneratorRegistry credentialGeneratorRegistry = new CredentialGeneratorRegistryImpl(claimsMapper, participantContextService, holderService, keyPairService);

    @Test
    void generate() {

        var generator = mock(CredentialGenerator.class);
        credentialGeneratorRegistry.addGenerator(CredentialFormat.VC1_0_JWT, generator);
        var definition = createCredentialDefinition();

        var participantContext = ParticipantContext.Builder.newInstance().participantContextId("participantContextId").apiTokenAlias("apiTokenAlias")
                .did("issuerDid")
                .build();

        var participant = new Holder("holderId", "participantDid", "name");

        var key = KeyPairResource.Builder.newInstance().id("keyId").keyId("keyId").privateKeyAlias("keyAlias").build();

        when(claimsMapper.apply(anyList(), any())).thenReturn(Result.success(Map.of()));
        when(participantContextService.getParticipantContext("participantContextId")).thenReturn(ServiceResult.success(participantContext));
        when(holderService.findById("holderId")).thenReturn(ServiceResult.success(participant));
        when(keyPairService.query(any())).thenReturn(ServiceResult.success(List.of(key)));
        when(generator.generateCredential(eq(definition), eq(key.getPrivateKeyAlias()), eq(key.getKeyId()), eq("issuerDid"), eq("participantDid"), any())).thenReturn(Result.success(mock()));
        var request = new CredentialGenerationRequest(definition, CredentialFormat.VC1_0_JWT);
        var result = credentialGeneratorRegistry.generateCredential("participantContextId", "holderId", request, Map.of());

        assertThat(result).isSucceeded();
    }

    @Test
    void generate_shouldFail_whenGeneratorNotFound() {


        var definition = createCredentialDefinition();

        when(claimsMapper.apply(anyList(), any())).thenReturn(Result.success(Map.of()));

        var request = new CredentialGenerationRequest(definition, CredentialFormat.VC1_0_JWT);
        var result = credentialGeneratorRegistry.generateCredential("participantContextId", "holderId", request, Map.of());

        assertThat(result).isFailed().detail().contains("No generator found for format VC1_0_JWT");
    }

    @Test
    void generate_shouldFail_ParticipantContextNotFound() {

        var generator = mock(CredentialGenerator.class);
        credentialGeneratorRegistry.addGenerator(CredentialFormat.VC1_0_JWT, generator);

        var definition = createCredentialDefinition();

        when(claimsMapper.apply(anyList(), any())).thenReturn(Result.success(Map.of()));
        when(participantContextService.getParticipantContext("participantContextId")).thenReturn(ServiceResult.notFound("not found"));

        var request = new CredentialGenerationRequest(definition, CredentialFormat.VC1_0_JWT);
        var result = credentialGeneratorRegistry.generateCredential("participantContextId", "holderId", request, Map.of());

        assertThat(result).isFailed().detail().contains("not found");
    }

    @Test
    void generate_shouldFail_whenParticipantNotFound() {

        var generator = mock(CredentialGenerator.class);
        credentialGeneratorRegistry.addGenerator(CredentialFormat.VC1_0_JWT, generator);

        var definition = createCredentialDefinition();

        var participantContext = ParticipantContext.Builder.newInstance().participantContextId("participantContextId").apiTokenAlias("apiTokenAlias")
                .did("issuerDid")
                .build();

        when(claimsMapper.apply(anyList(), any())).thenReturn(Result.success(Map.of()));
        when(participantContextService.getParticipantContext("participantContextId")).thenReturn(ServiceResult.success(participantContext));
        when(holderService.findById("holderId")).thenReturn(ServiceResult.notFound("not found"));

        var request = new CredentialGenerationRequest(definition, CredentialFormat.VC1_0_JWT);
        var result = credentialGeneratorRegistry.generateCredential("participantContextId", "holderId", request, Map.of());

        assertThat(result).isFailed().detail().contains("not found");
    }

    @Test
    void generate_shouldFail_whenNoKeysFound() {

        var generator = mock(CredentialGenerator.class);
        credentialGeneratorRegistry.addGenerator(CredentialFormat.VC1_0_JWT, generator);

        var definition = createCredentialDefinition();

        var participantContext = ParticipantContext.Builder.newInstance().participantContextId("participantContextId").apiTokenAlias("apiTokenAlias")
                .did("issuerDid")
                .build();

        var participant = new Holder("holderId", "participantDid", "name");


        when(claimsMapper.apply(anyList(), any())).thenReturn(Result.success(Map.of()));
        when(participantContextService.getParticipantContext("participantContextId")).thenReturn(ServiceResult.success(participantContext));
        when(holderService.findById("holderId")).thenReturn(ServiceResult.success(participant));
        when(keyPairService.query(any())).thenReturn(ServiceResult.success(List.of()));

        var request = new CredentialGenerationRequest(definition, CredentialFormat.VC1_0_JWT);
        var result = credentialGeneratorRegistry.generateCredential("participantContextId", "holderId", request, Map.of());

        assertThat(result).isFailed().detail().contains("No active key pair found");
    }

    @Test
    void generate_shouldFail_GenerationFails() {

        var generator = mock(CredentialGenerator.class);
        credentialGeneratorRegistry.addGenerator(CredentialFormat.VC1_0_JWT, generator);
        var definition = createCredentialDefinition();

        var participantContext = ParticipantContext.Builder.newInstance().participantContextId("participantContextId").apiTokenAlias("apiTokenAlias")
                .did("issuerDid")
                .build();

        var participant = new Holder("holderId", "participantDid", "name");

        var key = KeyPairResource.Builder.newInstance().id("keyId").keyId("keyId").privateKeyAlias("keyAlias").build();

        when(claimsMapper.apply(anyList(), any())).thenReturn(Result.success(Map.of()));
        when(participantContextService.getParticipantContext("participantContextId")).thenReturn(ServiceResult.success(participantContext));
        when(holderService.findById("holderId")).thenReturn(ServiceResult.success(participant));
        when(keyPairService.query(any())).thenReturn(ServiceResult.success(List.of(key)));
        when(generator.generateCredential(eq(definition), eq(key.getPrivateKeyAlias()), eq(key.getKeyId()), eq("issuerDid"), eq("participantDid"), any())).thenReturn(Result.failure("failed"));
        var request = new CredentialGenerationRequest(definition, CredentialFormat.VC1_0_JWT);
        var result = credentialGeneratorRegistry.generateCredential("participantContextId", "holderId", request, Map.of());

        assertThat(result).isFailed().detail().contains("failed");
    }

    private CredentialDefinition createCredentialDefinition() {
        return CredentialDefinition.Builder.newInstance()
                .credentialType("MembershipCredential")
                .mapping(new MappingDefinition("input", "outut", true))
                .jsonSchema("{}")
                .build();
    }
}
