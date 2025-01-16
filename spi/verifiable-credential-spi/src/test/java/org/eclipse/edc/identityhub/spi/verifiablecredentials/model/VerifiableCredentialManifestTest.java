/*
 *  Copyright (c) 2024 Amadeus IT Group.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus IT Group - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.verifiablecredentials.model;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class VerifiableCredentialManifestTest {

    private final TypeManager typeManager = new JacksonTypeManager();

    @Test
    void serDeser() {
        var manifest = VerifiableCredentialManifest.Builder.newInstance()
                .id("id")
                .participantContextId("participantId")
                .verifiableCredentialContainer(new VerifiableCredentialContainer("rawVc", CredentialFormat.JWT, VerifiableCredential.Builder.newInstance()
                        .type("type")
                        .credentialSubject(CredentialSubject.Builder.newInstance().id("id").claim("foo", "bar").build())
                        .issuer(new Issuer("issuer"))
                        .issuanceDate(Instant.now())
                        .build()))
                .build();

        var serialized = typeManager.writeValueAsString(manifest);

        var deserialized = typeManager.readValue(serialized, VerifiableCredentialManifest.class);

        assertThat(deserialized).usingRecursiveComparison().isEqualTo(manifest);
    }
}