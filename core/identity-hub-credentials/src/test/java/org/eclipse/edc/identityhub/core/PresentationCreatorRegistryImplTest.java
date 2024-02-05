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

package org.eclipse.edc.identityhub.core;

import org.eclipse.edc.identityhub.spi.KeyPairService;
import org.eclipse.edc.identityhub.spi.generator.PresentationGenerator;
import org.eclipse.edc.identityhub.spi.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.model.KeyPairState;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.ServiceResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked") // mocking a generic type (PresentationGenerator) would raise warnings
class PresentationCreatorRegistryImplTest {

    private static final String TEST_PARTICIPANT = "test-participant";
    private final KeyPairService keyPairService = mock();
    private final PresentationCreatorRegistryImpl registry = new PresentationCreatorRegistryImpl(keyPairService);

    @Test
    void createPresentation_whenSingleKey() {
        var keyPair = createKeyPair(TEST_PARTICIPANT, "key-1").build();
        when(keyPairService.query(any())).thenReturn(ServiceResult.success(List.of(keyPair)));

        var generator = mock(PresentationGenerator.class);
        registry.addCreator(generator, CredentialFormat.JWT);
        assertThatNoException().isThrownBy(() -> registry.createPresentation(TEST_PARTICIPANT, List.of(), CredentialFormat.JWT, Map.of()));
        verify(generator).generatePresentation(anyList(), eq(keyPair.getKeyId()), anyMap());
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
        verify(generator).generatePresentation(anyList(), argThat(s -> s.equals("key-1") || s.equals("key-2")), anyMap());
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
        verify(generator).generatePresentation(anyList(), eq("key-2"), anyMap());
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
                .state(KeyPairState.ACTIVE)
                .isDefaultPair(true)
                .privateKeyAlias(participantId + "-alias");
    }
}