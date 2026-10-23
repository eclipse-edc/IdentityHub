/*
 *  Copyright (c) 2026 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.publisher;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class LocalCredentialPublisherTest {

    private final LocalCredentialPublisher publisher = new LocalCredentialPublisher("https://issuer.example.com/statuslist");

    @Test
    void publicUrl() {
        assertThat(publisher.publicUrl("status-list-id")).contains("https://issuer.example.com/statuslist/status-list-id");
    }

    @Test
    void publish_whenIdentifiedByUrl_shouldPublishAtThatUrl() {
        var url = "https://issuer.example.com/statuslist/status-list-id";

        assertThat(publisher.publish(statusList(url))).isSucceeded().isEqualTo(url);
    }

    @Test
    void publish_whenIdentifiedByUuid_shouldPublishAtUrlWithThatUuid() {
        assertThat(publisher.publish(statusList("status-list-id"))).isSucceeded().isEqualTo("https://issuer.example.com/statuslist/status-list-id");
    }

    private VerifiableCredentialResource statusList(String credentialId) {
        var credential = VerifiableCredential.Builder.newInstance()
                .id(credentialId)
                .type("BitstringStatusListCredential")
                .credentialSubject(new CredentialSubject())
                .issuer(new Issuer("did:web:issuer"))
                .issuanceDate(Instant.now())
                .build();
        return VerifiableCredentialResource.Builder.newStatusList()
                .issuerId("did:web:issuer")
                .holderId("did:web:issuer")
                .credential(new VerifiableCredentialContainer("raw-token", CredentialFormat.VC2_0_JOSE, credential))
                .build();
    }
}
