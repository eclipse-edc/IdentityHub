/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identithub.verifiablepresentation;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.PresentationGenerator;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.ServiceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked") // mocking a generic type (PresentationGenerator) would raise warnings
class PresentationCreatorRegistryImplTest {

    public static final String ISSUER_ID = "did:web:test";
    private static final String TEST_PARTICIPANT = "test-participant";
    private final KeyPairService keyPairService = mock();
    private final ParticipantContextService participantContextService = mock();
    private final PresentationCreatorRegistryImpl registry = new PresentationCreatorRegistryImpl(keyPairService, participantContextService);

    @BeforeEach
    void setup() {
        when(participantContextService.getParticipantContext(anyString()))
                .thenReturn(ServiceResult.success(ParticipantContext.Builder.newInstance()
                        .participantId("test-participant")
                        .apiTokenAlias("test-token")
                        .did(ISSUER_ID).build()));
    }

    @Test
    void createPresentation_whenSingleKey() {
        var keyPair = createKeyPair(TEST_PARTICIPANT, "key-1").build();
        when(keyPairService.query(any())).thenReturn(ServiceResult.success(List.of(keyPair)));

        var generator = mock(PresentationGenerator.class);
        registry.addCreator(generator, CredentialFormat.JWT);
        assertThatNoException().isThrownBy(() -> registry.createPresentation(TEST_PARTICIPANT, List.of(), CredentialFormat.JWT, Map.of()));
        verify(generator).generatePresentation(anyList(), eq(keyPair.getPrivateKeyAlias()), eq(keyPair.getKeyId()), eq(ISSUER_ID), argThat(additional -> ISSUER_ID.equals(additional.get("controller"))));
    }

    @Test
    void createPresentation_whenKeyPairServiceReturnsFailure() {
        when(keyPairService.query(any())).thenReturn(ServiceResult.notFound("foobar"));
        var generator = mock(PresentationGenerator.class);
        registry.addCreator(generator, CredentialFormat.JWT);

        assertThatThrownBy(() -> registry.createPresentation(TEST_PARTICIPANT, List.of(), CredentialFormat.JWT, Map.of()))
                .isInstanceOf(EdcException.class)
                .hasMessage("Error obtaining private key for participant 'test-participant': foobar");
        verifyNoInteractions(generator);
    }

    @Test
    void createPresentation_whenNoDefaultKey() {
        var keyPair1 = createKeyPair(TEST_PARTICIPANT, "key-1").isDefaultPair(false).build();
        var keyPair2 = createKeyPair(TEST_PARTICIPANT, "key-2").isDefaultPair(false).build();
        when(keyPairService.query(any())).thenReturn(ServiceResult.success(List.of(keyPair1, keyPair2)));

        var generator = mock(PresentationGenerator.class);
        registry.addCreator(generator, CredentialFormat.JWT);
        assertThatNoException().isThrownBy(() -> registry.createPresentation(TEST_PARTICIPANT, List.of(), CredentialFormat.JWT, Map.of()));
        verify(generator).generatePresentation(anyList(),
                argThat(s -> s.equals(keyPair1.getPrivateKeyAlias()) || s.equals(keyPair2.getPrivateKeyAlias())),
                argThat(s -> s.equals(keyPair1.getKeyId()) || s.equals(keyPair2.getKeyId())),
                eq(ISSUER_ID), argThat(additional -> ISSUER_ID.equals(additional.get("controller"))));
    }


    @Test
    void createPresentation_whenDefaultKey() {
        var keyPair1 = createKeyPair(TEST_PARTICIPANT, "key-1").isDefaultPair(false).build();
        var keyPair2 = createKeyPair(TEST_PARTICIPANT, "key-2").isDefaultPair(true).build();
        var keyPair3 = createKeyPair(TEST_PARTICIPANT, "key-3").isDefaultPair(false).build();
        when(keyPairService.query(any())).thenReturn(ServiceResult.success(List.of(keyPair1, keyPair2, keyPair3)));

        var generator = mock(PresentationGenerator.class);
        registry.addCreator(generator, CredentialFormat.JWT);
        assertThatNoException().isThrownBy(() -> registry.createPresentation(TEST_PARTICIPANT, List.of(), CredentialFormat.JWT, Map.of()));
        verify(generator).generatePresentation(anyList(), eq(keyPair2.getPrivateKeyAlias()), eq(keyPair2.getKeyId()), eq(ISSUER_ID), argThat(additional -> ISSUER_ID.equals(additional.get("controller"))));
    }

    @Test
    void createPresentation_whenNoActiveKey() {
        when(keyPairService.query(any())).thenReturn(ServiceResult.success(List.of()));

        var generator = mock(PresentationGenerator.class);
        registry.addCreator(generator, CredentialFormat.JWT);
        assertThatThrownBy(() -> registry.createPresentation(TEST_PARTICIPANT, List.of(), CredentialFormat.JWT, Map.of()))
                .isInstanceOf(EdcException.class)
                .hasMessage("No active key pair found for participant 'test-participant'");
        verifyNoInteractions(generator);
    }

    private KeyPairResource.Builder createKeyPair(String participantId, String keyId) {
        return KeyPairResource.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .keyId(keyId)
                .state(KeyPairState.ACTIVATED)
                .isDefaultPair(true)
                .privateKeyAlias("%s-%s-alias".formatted(participantId, keyId));
    }
}