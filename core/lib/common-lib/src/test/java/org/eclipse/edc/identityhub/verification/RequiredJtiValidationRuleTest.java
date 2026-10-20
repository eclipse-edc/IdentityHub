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

package org.eclipse.edc.identityhub.verification;

import org.eclipse.edc.jwt.validation.jti.JtiValidationEntry;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.StoreResult;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RequiredJtiValidationRuleTest {

    private final JtiValidationStore jtiValidationStore = mock();
    private final RequiredJtiValidationRule rule = new RequiredJtiValidationRule(jtiValidationStore, mock());

    @Test
    void checkRule_whenJtiNotUsedBefore() {
        when(jtiValidationStore.storeEntry(any())).thenReturn(StoreResult.success());

        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().claim("jti", "token-id").build(), null)).isSucceeded();
        verify(jtiValidationStore).storeEntry(argThat((JtiValidationEntry entry) -> entry.tokenId().equals("token-id")));
    }

    @Test
    void checkRule_whenJtiUsedBefore() {
        when(jtiValidationStore.storeEntry(any())).thenReturn(StoreResult.alreadyExists("exists"));

        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().claim("jti", "token-id").build(), null)).isFailed()
                .detail().contains("already used");
    }

    @Test
    void checkRule_whenJtiMissing() {
        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().claim("iss", "did:web:acme.com").build(), null)).isFailed()
                .detail().isEqualTo("Required claim 'jti' not present on token.");
        verifyNoInteractions(jtiValidationStore);
    }
}
