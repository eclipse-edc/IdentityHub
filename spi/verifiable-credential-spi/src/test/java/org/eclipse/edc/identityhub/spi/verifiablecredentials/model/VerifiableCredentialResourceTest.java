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

package org.eclipse.edc.identityhub.spi.verifiablecredentials.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerifiableCredentialResourceTest {

    @Test
    void verifyBuilder_whenInvalidRequiredProperties() {
        // missing holder and issuer
        assertThatThrownBy(() -> VerifiableCredentialResource.Builder.newInstance().build()).isInstanceOf(NullPointerException.class);
        // missing issuer
        assertThatThrownBy(() -> VerifiableCredentialResource.Builder.newInstance()
                .holderId("test-holder")
                .build()).isInstanceOf(NullPointerException.class);

        //missing holder
        assertThatThrownBy(() -> VerifiableCredentialResource.Builder.newInstance()
                .issuerId("test-issuer")
                .build()).isInstanceOf(NullPointerException.class);
    }

    @Test
    void verifyBuilder_assertDefaultValues() {
        var vc = VerifiableCredentialResource.Builder.newInstance()
                .issuerId("test-issuer")
                .holderId("test-holder")
                .participantContextId("test-participant")
                .build();

        assertThat(vc.getClock()).isNotNull();
        assertThat(vc.getId()).isNotNull();
        assertThat(vc.getStateAsEnum()).isEqualTo(VcStatus.INITIAL);
        assertThat(vc.getTimeOfLastStatusUpdate()).isNull();
    }
}