/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.issuance.attestation.holder;

import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationContext;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class HolderAttestationFactoryTest {

    @Test
    void shouldReturnHolderProperties() {
        var factory = new HolderAttestationFactory();
        var participantContextId = UUID.randomUUID().toString();
        var attestationDefinition = AttestationDefinition.Builder.newInstance().id(UUID.randomUUID().toString())
                .attestationType("holder").participantContextId(participantContextId).build();
        var source = factory.createSource(attestationDefinition);
        var properties = Map.<String, Object>of("key", "value");
        var holder = Holder.Builder.newInstance().holderId(UUID.randomUUID().toString()).did(UUID.randomUUID().toString())
                .participantContextId(participantContextId).properties(properties).build();

        var result = source.execute(new TestHolderAttestationContext(holder));

        assertThat(result).isSucceeded().isSameAs(properties);
    }

    private record TestHolderAttestationContext(Holder holder) implements AttestationContext {

        @Override
        public @Nullable ClaimToken getClaimToken(String type) {
            return null;
        }

        @Override
        public String participantContextId() {
            return "";
        }
    }
}
