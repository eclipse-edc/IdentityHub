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

package org.eclipse.edc.identityhub.issuance.credentials.attestation;

import org.eclipse.edc.identityhub.spi.issuance.credentials.attestation.AttestationContext;
import org.eclipse.edc.identityhub.spi.issuance.credentials.attestation.AttestationDefinitionStore;
import org.eclipse.edc.identityhub.spi.issuance.credentials.attestation.AttestationSource;
import org.eclipse.edc.identityhub.spi.issuance.credentials.attestation.AttestationSourceFactory;
import org.eclipse.edc.identityhub.spi.issuance.credentials.model.AttestationDefinition;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
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
    void verify_pipeline() {
        var attestationDefinition = new AttestationDefinition("a123", "testType", Map.of());

        var store = mock(AttestationDefinitionStore.class);
        when(store.resolveDefinition(eq("a123"))).thenReturn(attestationDefinition);

        var pipeline = new AttestationPipelineImpl(store);

        var attestationSource = mock(AttestationSource.class);
        when(attestationSource.execute(isA(AttestationContext.class))).thenReturn(success(Map.of("test", "value")));

        var sourceFactory = mock(AttestationSourceFactory.class);
        when(sourceFactory.createSource(isA(AttestationDefinition.class))).thenReturn(attestationSource);

        pipeline.registerFactory("testType", sourceFactory);

        var results = pipeline.evaluate(Set.of("a123"), new DefaultAttestationContext("123", emptyMap()));
        assertThat(results.succeeded()).isTrue();
        assertThat(results.getContent()).contains(entry("test", "value"));


        verify(store).resolveDefinition("a123");
        verify(attestationSource).execute(isA(AttestationContext.class));
        verify(sourceFactory).createSource(isA(AttestationDefinition.class));
    }


    @Test
    void verify_failFast() {
        var attestationDefinition1 = new AttestationDefinition("a123", "testType1", Map.of());
        var attestationDefinition2 = new AttestationDefinition("a456", "testType1", Map.of());

        var store = mock(AttestationDefinitionStore.class);
        when(store.resolveDefinition(eq("a123"))).thenReturn(attestationDefinition1);
        when(store.resolveDefinition(eq("a456"))).thenReturn(attestationDefinition2);

        var pipeline = new AttestationPipelineImpl(store);

        var failedSource = mock(AttestationSource.class);
        when(failedSource.execute(isA(AttestationContext.class))).thenReturn(failure(""));

        var sourceFactory = mock(AttestationSourceFactory.class);
        when(sourceFactory.createSource(isA(AttestationDefinition.class))).thenReturn(failedSource);

        pipeline.registerFactory("testType1", sourceFactory);

        var results = pipeline.evaluate(Set.of("a123", "a456"), new DefaultAttestationContext("123", emptyMap()));
        assertThat(results.failed()).isTrue();

        verify(store).resolveDefinition("a123");
        verify(sourceFactory, times(1)).createSource(isA(AttestationDefinition.class));
        verify(failedSource, times(1)).execute(isA(AttestationContext.class));
    }


}