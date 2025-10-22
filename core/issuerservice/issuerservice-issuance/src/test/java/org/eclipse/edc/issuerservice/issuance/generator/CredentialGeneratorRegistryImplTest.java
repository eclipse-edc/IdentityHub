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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceResult.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CredentialGeneratorRegistryImplTest {


    private final IssuanceClaimsMapper claimsMapper = mock();
    private final ParticipantContextService participantContextService = mock();
    private final HolderService holderService = mock();
    private final KeyPairService keyPairService = mock();
    private final CredentialGeneratorRegistry credentialGeneratorRegistry = new CredentialGeneratorRegistryImpl(claimsMapper, participantContextService, holderService, keyPairService);

    @Test
    void generate_whenSingleKey_shouldSucceed() {

        var generator = mock(CredentialGenerator.class);
        credentialGeneratorRegistry.addGenerator(VC1_0_JWT, generator);
        var definition = createCredentialDefinition();

        var participantContext = ParticipantContext.Builder.newInstance().participantContextId("participantContextId").apiTokenAlias("apiTokenAlias")
                .did("issuerDid")
                .build();

        var participant = createHolder();

        var key = KeyPairResource.Builder.newCredentialSigning().id("keyId").keyId("keyId").privateKeyAlias("keyAlias").build();

        when(claimsMapper.apply(anyList(), any())).thenReturn(Result.success(Map.of()));
        when(participantContextService.getParticipantContext("participantContextId")).thenReturn(success(participantContext));
        when(holderService.findById("holderId")).thenReturn(success(participant));
        when(keyPairService.query(any())).thenReturn(success(List.of(key)));
        when(generator.generateCredential(eq(definition), eq(key.getPrivateKeyAlias()), eq(key.getKeyId()), eq("issuerDid"), eq("participantDid"), any())).thenReturn(Result.success(mock()));
        var request = new CredentialGenerationRequest(definition, VC1_0_JWT);
        var result = credentialGeneratorRegistry.generateCredential("participantContextId", "holderId", request, Map.of());

        assertThat(result).isSucceeded();
    }

    @Test
    void generate_whenGeneratorNotFound_shouldFail() {


        var definition = createCredentialDefinition();

        when(claimsMapper.apply(anyList(), any())).thenReturn(Result.success(Map.of()));

        var request = new CredentialGenerationRequest(definition, VC1_0_JWT);
        var result = credentialGeneratorRegistry.generateCredential("participantContextId", "holderId", request, Map.of());

        assertThat(result).isFailed().detail().contains("No generator found for format VC1_0_JWT");
    }

    @Test
    void generate_ParticipantContextNotFound_shouldFail() {

        var generator = mock(CredentialGenerator.class);
        credentialGeneratorRegistry.addGenerator(VC1_0_JWT, generator);

        var definition = createCredentialDefinition();

        when(claimsMapper.apply(anyList(), any())).thenReturn(Result.success(Map.of()));
        when(participantContextService.getParticipantContext("participantContextId")).thenReturn(ServiceResult.notFound("not found"));

        var request = new CredentialGenerationRequest(definition, VC1_0_JWT);
        var result = credentialGeneratorRegistry.generateCredential("participantContextId", "holderId", request, Map.of());

        assertThat(result).isFailed().detail().contains("not found");
    }

    @Test
    void generate_whenParticipantNotFound_shouldFail() {

        var generator = mock(CredentialGenerator.class);
        credentialGeneratorRegistry.addGenerator(VC1_0_JWT, generator);

        var definition = createCredentialDefinition();

        var participantContext = ParticipantContext.Builder.newInstance().participantContextId("participantContextId").apiTokenAlias("apiTokenAlias")
                .did("issuerDid")
                .build();

        when(claimsMapper.apply(anyList(), any())).thenReturn(Result.success(Map.of()));
        when(participantContextService.getParticipantContext("participantContextId")).thenReturn(success(participantContext));
        when(holderService.findById("holderId")).thenReturn(ServiceResult.notFound("not found"));

        var request = new CredentialGenerationRequest(definition, VC1_0_JWT);
        var result = credentialGeneratorRegistry.generateCredential("participantContextId", "holderId", request, Map.of());

        assertThat(result).isFailed().detail().contains("not found");
    }

    @Test
    void generate_whenNoKeysFound_shouldFail() {

        var generator = mock(CredentialGenerator.class);
        credentialGeneratorRegistry.addGenerator(VC1_0_JWT, generator);

        var definition = createCredentialDefinition();

        var participantContext = ParticipantContext.Builder.newInstance().participantContextId("participantContextId").apiTokenAlias("apiTokenAlias")
                .did("issuerDid")
                .build();

        var participant = createHolder();


        when(claimsMapper.apply(anyList(), any())).thenReturn(Result.success(Map.of()));
        when(participantContextService.getParticipantContext("participantContextId")).thenReturn(success(participantContext));
        when(holderService.findById("holderId")).thenReturn(success(participant));
        when(keyPairService.query(any())).thenReturn(success(List.of()));

        var request = new CredentialGenerationRequest(definition, VC1_0_JWT);
        var result = credentialGeneratorRegistry.generateCredential("participantContextId", "holderId", request, Map.of());

        assertThat(result).isFailed().detail().contains("No active key pair found");
    }

    @Test
    void generate_whenSingleKeyWrongUsage_shouldFail() {
        var now = Instant.now();
        var credential = createCredential(now).build();
        var generator = mock(CredentialGenerator.class);
        when(generator.signCredential(any(), any(), any())).thenReturn(Result.success("some-token"));
        credentialGeneratorRegistry.addGenerator(CredentialFormat.VC2_0_JOSE, generator);

        var key = KeyPairResource.Builder.newAccessToken().id("keyId").keyId("keyId").privateKeyAlias("keyAlias").build();
        when(keyPairService.query(any())).thenReturn(success(List.of(key)));

        var result = credentialGeneratorRegistry.signCredential("test-participant", credential, CredentialFormat.VC2_0_JOSE);

        assertThat(result).isFailed()
                .detail().isEqualTo("No active key pair found for participant 'test-participant' with usage 'CREDENTIAL_SIGNING'");

        verify(keyPairService).query(any());
        verifyNoMoreInteractions(participantContextService, keyPairService, generator, holderService, claimsMapper);
    }

    @Test
    void generate_whenMultipleKeys_noDefault_shouldFail() {
        var generator = mock(CredentialGenerator.class);
        credentialGeneratorRegistry.addGenerator(VC1_0_JWT, generator);
        var definition = createCredentialDefinition();

        var participantContext = ParticipantContext.Builder.newInstance()
                .participantContextId("participantContextId")
                .apiTokenAlias("apiTokenAlias")
                .did("issuerDid")
                .build();

        var participant = createHolder();

        var key1 = KeyPairResource.Builder.newCredentialSigning().id("keyId1").keyId("keyId1").privateKeyAlias("keyAlias1").isDefaultPair(false).build();
        var key2 = KeyPairResource.Builder.newCredentialSigning().id("keyId2").keyId("keyId2").privateKeyAlias("keyAlias2").isDefaultPair(false).build();

        when(claimsMapper.apply(anyList(), any())).thenReturn(Result.success(Map.of()));
        when(participantContextService.getParticipantContext("participantContextId")).thenReturn(success(participantContext));
        when(holderService.findById("holderId")).thenReturn(success(participant));
        when(keyPairService.query(any())).thenReturn(success(List.of(key1, key2)));
        var request = new CredentialGenerationRequest(definition, VC1_0_JWT);
        var result = credentialGeneratorRegistry.generateCredential("participantContextId", "holderId", request, Map.of());

        assertThat(result).isFailed();
        verifyNoMoreInteractions(generator);
    }

    @Test
    void generate_whenMultipleKeysWrongUsage_shouldFail() {
        var generator = mock(CredentialGenerator.class);
        credentialGeneratorRegistry.addGenerator(VC1_0_JWT, generator);
        var definition = createCredentialDefinition();

        var participantContext = ParticipantContext.Builder.newInstance()
                .participantContextId("participantContextId")
                .apiTokenAlias("apiTokenAlias")
                .did("issuerDid")
                .build();

        var participant = createHolder();

        var key1 = KeyPairResource.Builder.newAccessToken().id("keyId1").keyId("keyId1").privateKeyAlias("keyAlias1").isDefaultPair(true).build();
        var key2 = KeyPairResource.Builder.newIdToken().id("keyId2").keyId("keyId2").privateKeyAlias("keyAlias2").isDefaultPair(false).build();

        when(claimsMapper.apply(anyList(), any())).thenReturn(Result.success(Map.of()));
        when(participantContextService.getParticipantContext("participantContextId")).thenReturn(success(participantContext));
        when(holderService.findById("holderId")).thenReturn(success(participant));
        when(keyPairService.query(any())).thenReturn(success(List.of(key1, key2)));
        var request = new CredentialGenerationRequest(definition, VC1_0_JWT);
        var result = credentialGeneratorRegistry.generateCredential("participantContextId", "holderId", request, Map.of());

        assertThat(result).isFailed().detail().isEqualTo("No active key pair found for participant 'participantContextId' with usage 'CREDENTIAL_SIGNING'");
        verifyNoMoreInteractions(generator);
    }

    @Test
    void generate_generationFails_shouldFail() {

        var generator = mock(CredentialGenerator.class);
        credentialGeneratorRegistry.addGenerator(VC1_0_JWT, generator);
        var definition = createCredentialDefinition();

        var participantContext = ParticipantContext.Builder.newInstance().participantContextId("participantContextId").apiTokenAlias("apiTokenAlias")
                .did("issuerDid")
                .build();

        var participant = createHolder();

        var key = KeyPairResource.Builder.newCredentialSigning().id("keyId").keyId("keyId").privateKeyAlias("keyAlias").build();

        when(claimsMapper.apply(anyList(), any())).thenReturn(Result.success(Map.of()));
        when(participantContextService.getParticipantContext("participantContextId")).thenReturn(success(participantContext));
        when(holderService.findById("holderId")).thenReturn(success(participant));
        when(keyPairService.query(any())).thenReturn(success(List.of(key)));
        when(generator.generateCredential(eq(definition), eq(key.getPrivateKeyAlias()), eq(key.getKeyId()), eq("issuerDid"), eq("participantDid"), any())).thenReturn(Result.failure("failed"));
        var request = new CredentialGenerationRequest(definition, VC1_0_JWT);
        var result = credentialGeneratorRegistry.generateCredential("participantContextId", "holderId", request, Map.of());

        assertThat(result).isFailed().detail().contains("failed");
    }

    @Test
    void signCredential() {
        var now = Instant.now();
        var credential = createCredential(now).build();
        var generator = mock(CredentialGenerator.class);
        when(generator.signCredential(any(), any(), any())).thenReturn(Result.success("some-token"));
        credentialGeneratorRegistry.addGenerator(CredentialFormat.VC2_0_JOSE, generator);

        var key = KeyPairResource.Builder.newCredentialSigning().id("keyId").keyId("keyId").privateKeyAlias("keyAlias").build();
        when(keyPairService.query(any())).thenReturn(success(List.of(key)));

        var result = credentialGeneratorRegistry.signCredential("test-participant", credential, CredentialFormat.VC2_0_JOSE);

        assertThat(result).isSucceeded()
                .satisfies(vc -> {
                    assertThat(vc.format()).isEqualTo(CredentialFormat.VC2_0_JOSE);
                    assertThat(vc.credential()).usingRecursiveComparison().isEqualTo(credential);
                    assertThat(vc.rawVc()).isEqualTo("some-token");
                });

        verify(generator).signCredential(any(), any(), any());
        verify(keyPairService).query(any());
        verifyNoMoreInteractions(participantContextService, keyPairService, generator, holderService, claimsMapper);
    }

    @Test
    void signCredential_whenKeyNotFound() {
        var now = Instant.now();
        var credential = createCredential(now)
                .build();
        var generator = mock(CredentialGenerator.class);
        when(generator.signCredential(any(), any(), any())).thenReturn(Result.success("some-token"));
        credentialGeneratorRegistry.addGenerator(CredentialFormat.VC2_0_JOSE, generator);

        when(keyPairService.query(any())).thenReturn(ServiceResult.notFound("foobar"));

        var result = credentialGeneratorRegistry.signCredential("test-participant", credential, CredentialFormat.VC2_0_JOSE);

        assertThat(result).isFailed()
                .detail().contains("foobar");
        verify(keyPairService).query(any());
        verifyNoMoreInteractions(participantContextService, keyPairService, generator, holderService, claimsMapper);
    }

    private VerifiableCredential.Builder createCredential(Instant now) {
        return VerifiableCredential.Builder.newInstance()
                .type("TestCredential")
                .id(UUID.randomUUID().toString())
                .issuer(new Issuer("did:web:issuer"))
                .issuanceDate(now)
                .expirationDate(now.plusSeconds(3600))
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id(UUID.randomUUID().toString())
                        .claim("foo", "bar")
                        .build());
    }

    private Holder createHolder() {
        return Holder.Builder.newInstance()
                .participantContextId(UUID.randomUUID().toString())
                .holderId("holderId")
                .did("participantDid")
                .holderName("name")
                .build();
    }

    private CredentialDefinition createCredentialDefinition() {
        return CredentialDefinition.Builder.newInstance()
                .credentialType("MembershipCredential")
                .mapping(new MappingDefinition("input", "outut", true))
                .participantContextId("participantContextId")
                .jsonSchema("{}")
                .formatFrom(VC1_0_JWT)
                .build();
    }
}
