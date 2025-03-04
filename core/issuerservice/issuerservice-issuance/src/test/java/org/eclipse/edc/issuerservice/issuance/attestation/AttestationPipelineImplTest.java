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

package org.eclipse.edc.issuerservice.issuance.attestation;

import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationContext;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSource;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactory;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttestationPipelineImplTest {

    @Test
    void evaluate_whenSingle_success() {
        var attestationDefinition = createAttestationDefinition("a123", "testType", Map.of());

        var store = mock(AttestationDefinitionStore.class);
        var attestationContext = mock(AttestationContext.class);

        when(store.resolveDefinition(eq("a123"))).thenReturn(attestationDefinition);

        var pipeline = new AttestationPipelineImpl(store);

        var attestationSource = mock(AttestationSource.class);
        when(attestationSource.execute(isA(AttestationContext.class))).thenReturn(success(Map.of("test", "value")));

        var sourceFactory = mock(AttestationSourceFactory.class);
        when(sourceFactory.createSource(isA(AttestationDefinition.class))).thenReturn(attestationSource);

        pipeline.registerFactory("testType", sourceFactory);

        var results = pipeline.evaluate(Set.of("a123"), attestationContext);
        assertThat(results).isSucceeded();
        assertThat(results.getContent()).contains(entry("test", "value"));


        verify(store).resolveDefinition("a123");
        verify(attestationSource).execute(isA(AttestationContext.class));
        verify(sourceFactory).createSource(isA(AttestationDefinition.class));
    }


    @Test
    void evaluate_whenMultipleInvalid_shouldFailOnFirst() {
        var attestationDefinition1 = createAttestationDefinition("a123", "testType1", Map.of());
        var attestationDefinition2 = createAttestationDefinition("a456", "testType1", Map.of());

        var store = mock(AttestationDefinitionStore.class);
        var attestationContext = mock(AttestationContext.class);
        when(store.resolveDefinition(eq("a123"))).thenReturn(attestationDefinition1);
        when(store.resolveDefinition(eq("a456"))).thenReturn(attestationDefinition2);

        var pipeline = new AttestationPipelineImpl(store);

        var failedSource = mock(AttestationSource.class);
        when(failedSource.execute(isA(AttestationContext.class))).thenReturn(failure(""));

        var sourceFactory = mock(AttestationSourceFactory.class);
        when(sourceFactory.createSource(isA(AttestationDefinition.class))).thenReturn(failedSource);

        pipeline.registerFactory("testType1", sourceFactory);

        var results = pipeline.evaluate(new LinkedHashSet<>(List.of("a123", "a456")), attestationContext);
        assertThat(results).isFailed();

        verify(store).resolveDefinition("a123");
        verify(sourceFactory, times(1)).createSource(isA(AttestationDefinition.class));
        verify(failedSource, times(1)).execute(isA(AttestationContext.class));
    }

    private AttestationDefinition createAttestationDefinition(String id, String type, Map<String, Object> configuration) {
        return AttestationDefinition.Builder.newInstance()
                .id(id)
                .attestationType(type)
                .participantContextId(UUID.randomUUID().toString())
                .configuration(configuration).build();
    }

}