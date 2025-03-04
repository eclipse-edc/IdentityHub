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

package org.eclipse.edc.issuerservice.issuance.attestations.presentation;

import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PresentationAttestationSourceFactoryTest {

    @Test
    void verify_create() {
        var factory = new PresentationAttestationSourceFactory();
        Map<String, Object> configuration = Map.of("credentialType", "testCred", "outputClaim", "testClaim");
        var definition = createAttestationDefinition("123", "presentation", configuration);

        var source = factory.createSource(definition);

        var claims = Map.of("testCred", ClaimToken.Builder.newInstance().build());

        assertThat(source.execute(new TestAttestationContext("participant", claims)).succeeded()).isTrue();
    }

    @Test
    void verify_create_optional() {
        var factory = new PresentationAttestationSourceFactory();
        Map<String, Object> configuration = Map.of("credentialType", "optionalCred", "outputClaim", "testClaim", "required", false);
        var definition = createAttestationDefinition("123", "presentation", configuration);

        var source = factory.createSource(definition);

        assertThat(source.execute(new TestAttestationContext("participant", Map.of())).succeeded()).isTrue();
    }

    private AttestationDefinition createAttestationDefinition(String id, String type, Map<String, Object> configuration) {
        return AttestationDefinition.Builder.newInstance()
                .id(id)
                .attestationType(type)
                .participantContextId(UUID.randomUUID().toString())
                .configuration(configuration).build();
    }

}