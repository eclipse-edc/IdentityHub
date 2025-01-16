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

package org.eclipse.edc.identityhub.api.verifiablecredentials.v1.unstable.transformer;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialManifest;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class VerifiableCredentialManifestToVerifiableCredentialResourceTransformerTest {

    private final VerifiableCredentialManifestToVerifiableCredentialResourceTransformer transformer = new VerifiableCredentialManifestToVerifiableCredentialResourceTransformer();

    @Test
    void transform_success() {
        var credential = VerifiableCredential.Builder.newInstance()
                .id("id")
                .type("type")
                .issuer(new Issuer("issuer"))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance().id("subject").claim("foo", "bar").build())
                .build();
        var manifest = VerifiableCredentialManifest.Builder.newInstance()
                .id("id")
                .participantContextId("participantId")
                .issuancePolicy(mock())
                .reissuancePolicy(mock())
                .verifiableCredentialContainer(new VerifiableCredentialContainer("rawVc", CredentialFormat.JWT, credential))
                .build();

        var resource = transformer.transform(manifest, null);

        assertNotNull(resource);
        assertThat(resource.getId()).isEqualTo(manifest.getId());
        assertThat(resource.getParticipantContextId()).isEqualTo(manifest.getParticipantContextId());
        assertThat(resource.getIssuancePolicy()).isEqualTo(manifest.getIssuancePolicy());
        assertThat(resource.getReissuancePolicy()).isEqualTo(manifest.getReissuancePolicy());
        assertThat(resource.getVerifiableCredential()).isEqualTo(manifest.getVerifiableCredentialContainer());
        assertThat(resource.getIssuerId()).isEqualTo(credential.getIssuer().id());
        assertThat(resource.getHolderId()).isEqualTo(credential.getCredentialSubject().stream().findFirst().get().getId());
    }

}