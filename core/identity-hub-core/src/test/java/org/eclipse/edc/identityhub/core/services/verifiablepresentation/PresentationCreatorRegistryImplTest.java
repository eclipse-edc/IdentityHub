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

package org.eclipse.edc.identityhub.core.services.verifiablepresentation;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.PresentationGenerator;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.identityhub.spi.participantcontext.model.KeyPairUsage.PRESENTATION_SIGNING;
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
    private final IdentityHubParticipantContextService participantContextService = mock();
    private final PresentationCreatorRegistryImpl registry = new PresentationCreatorRegistryImpl(keyPairService, participantContextService, new NoopTransactionContext());

    @BeforeEach
    void setup() {
        when(participantContextService.getParticipantContext(anyString()))
                .thenReturn(ServiceResult.success(IdentityHubParticipantContext.Builder.newInstance()
                        .participantContextId("test-participant")
                        .apiTokenAlias("test-token")
                        .did(ISSUER_ID).build()));
    }

    @Test
    void createPresentation_whenSingleKey() {
        var keyPair = createKeyPair("key-1").build();
        when(keyPairService.getActiveKeyPairForUsage(anyString(), eq(PRESENTATION_SIGNING))).thenReturn(ServiceResult.success(keyPair));

        var generator = mock(PresentationGenerator.class);
        registry.addCreator(generator, CredentialFormat.VC1_0_JWT);
        assertThatNoException().isThrownBy(() -> registry.createPresentation(TEST_PARTICIPANT, List.of(), CredentialFormat.VC1_0_JWT, Map.of()));
        verify(generator).generatePresentation(eq(TEST_PARTICIPANT), anyList(), eq(keyPair.getPrivateKeyAlias()), eq(keyPair.getKeyId()), eq(ISSUER_ID), argThat(additional -> ISSUER_ID.equals(additional.get("controller"))));
    }

    @Test
    void createPresentation_whenKeyPairServiceReturnsFailure() {
        when(keyPairService.getActiveKeyPairForUsage(anyString(), eq(PRESENTATION_SIGNING))).thenReturn(ServiceResult.notFound("foobar"));
        var generator = mock(PresentationGenerator.class);
        registry.addCreator(generator, CredentialFormat.VC1_0_JWT);

        assertThatThrownBy(() -> registry.createPresentation(TEST_PARTICIPANT, List.of(), CredentialFormat.VC1_0_JWT, Map.of()))
                .isInstanceOf(EdcException.class)
                .hasMessage("foobar");
        verifyNoInteractions(generator);
    }


    @Test
    void createPresentation_whenNoActiveKey() {
        when(keyPairService.getActiveKeyPairForUsage(anyString(), eq(PRESENTATION_SIGNING))).thenReturn(ServiceResult.notFound("foo msg"));

        var generator = mock(PresentationGenerator.class);
        registry.addCreator(generator, CredentialFormat.VC1_0_JWT);
        assertThatThrownBy(() -> registry.createPresentation(TEST_PARTICIPANT, List.of(), CredentialFormat.VC1_0_JWT, Map.of()))
                .isInstanceOf(EdcException.class)
                .hasMessageStartingWith("foo msg");
        verifyNoInteractions(generator);
    }

    private KeyPairResource.Builder createKeyPair(String keyId) {
        return KeyPairResource.Builder.newPresentationSigning()
                .id(UUID.randomUUID().toString())
                .keyId(keyId)
                .state(KeyPairState.ACTIVATED)
                .isDefaultPair(true)
                .privateKeyAlias("%s-%s-alias".formatted(PresentationCreatorRegistryImplTest.TEST_PARTICIPANT, keyId));
    }
}