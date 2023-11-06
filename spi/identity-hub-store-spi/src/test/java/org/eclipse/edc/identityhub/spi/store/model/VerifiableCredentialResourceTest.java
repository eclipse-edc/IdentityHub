/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.store.model;

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
                .build();

        assertThat(vc.getClock()).isNotNull();
        assertThat(vc.id).isNotNull();
        assertThat(vc.getState()).isEqualTo(VcState.INITIAL);
    }
}