/*
 *  Copyright (c) 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.credentials.model;

import org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ProofTest extends AbstractSerDeserTest<Proof> {

    @Override
    protected Class<Proof> getClazz() {
        return Proof.class;
    }

    @Override
    protected Proof getEntity() {
        return VerifiableCredentialTestUtil.generateProof();
    }

    @Test
    void verifyTypeMandatory() {
        assertThatNullPointerException().isThrownBy(() -> Proof.Builder.newInstance().build())
                .withMessageContaining("`type`");
    }

    @Test
    void verifyCreatedMandatory() {
        assertThatNullPointerException().isThrownBy(() -> Proof.Builder.newInstance().type("type").build())
                .withMessageContaining("`created`");
    }

    @Test
    void verifyVerificationMethodMandatory() {
        assertThatNullPointerException().isThrownBy(() -> Proof.Builder.newInstance()
                        .type("type")
                        .created(new Date())
                        .build())
                .withMessageContaining("`verificationMethod`");
    }

    @Test
    void verifyProofPurposeMandatory() {
        assertThatNullPointerException().isThrownBy(() -> Proof.Builder.newInstance()
                        .type("type")
                        .created(new Date())
                        .verificationMethod("verificationMethod")
                        .build())
                .withMessageContaining("`proofPurpose`");
    }
}