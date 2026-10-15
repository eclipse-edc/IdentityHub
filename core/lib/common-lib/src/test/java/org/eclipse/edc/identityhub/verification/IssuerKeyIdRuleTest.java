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

import org.eclipse.edc.spi.iam.ClaimToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class IssuerKeyIdRuleTest {

    private static ClaimToken issuer(String iss) {
        return ClaimToken.Builder.newInstance().claim("iss", iss).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"did:web:wallet.acme.com", "did:web:localhost%3A7083:holder", "did:example:123"})
    void checkRule_whenKidBelongsToIssuer(String iss) {
        assertThat(new IssuerKeyIdRule(iss + "#key-1").checkRule(issuer(iss), null)).isSucceeded();
    }

    @Test
    void checkRule_whenKidFragmentContainsSeparator() {
        assertThat(new IssuerKeyIdRule("did:web:acme.com#key#1").checkRule(issuer("did:web:acme.com"), null)).isSucceeded();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "did:web:walletxacme.com#key-1", // '.' in the issuer must not match any character
            "did:web:wallet.acme.com.evil#key-1",
            "did:web:evil:did:web:wallet.acme.com#key-1",
            "did:web:wallet.acme.com", // no fragment
            "did:web:wallet.acme.com#", // empty fragment
            "#key-1"
    })
    void checkRule_whenKidDoesNotBelongToIssuer(String kid) {
        assertThat(new IssuerKeyIdRule(kid).checkRule(issuer("did:web:wallet.acme.com"), null)).isFailed()
                .detail().startsWith("kid header");
    }

    @Test
    void checkRule_whenIssuerMissing() {
        assertThat(new IssuerKeyIdRule("did:web:acme.com#key-1").checkRule(ClaimToken.Builder.newInstance().build(), null)).isFailed()
                .detail().isEqualTo("Claim 'iss' is missing.");
    }

    @Test
    void checkRule_whenKidMissing() {
        assertThat(new IssuerKeyIdRule(null).checkRule(issuer("did:web:acme.com"), null)).isFailed()
                .detail().isEqualTo("Header 'kid' is missing.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"did:web:a[", "did:web:.*", "acme.com", "did:web:acme com", "DID:web:acme.com"})
    void checkRule_whenIssuerInvalidDid(String iss) {
        assertThatNoException().isThrownBy(() -> assertThat(new IssuerKeyIdRule(iss + "#key-1").checkRule(issuer(iss), null)).isFailed()
                .detail().isEqualTo("Claim 'iss' is not a valid DID."));
    }

    @Test
    void checkRule_whenIssuerIsCatastrophicPattern() {
        var iss = "(a+)+$";
        var kid = "a".repeat(64) + "!#key-1";
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> assertThat(new IssuerKeyIdRule(kid).checkRule(issuer(iss), null)).isFailed());
    }

    @Test
    void checkRule_whenIssuerTooLong() {
        var iss = "did:web:" + "a".repeat(IssuerKeyIdRule.MAX_LENGTH);
        assertThat(new IssuerKeyIdRule("did:web:acme.com#key-1").checkRule(issuer(iss), null)).isFailed()
                .detail().contains("exceeds the maximum length");
    }

    @Test
    void checkRule_whenKidTooLong() {
        var iss = "did:web:acme.com";
        var kid = iss + "#" + "k".repeat(IssuerKeyIdRule.MAX_LENGTH);
        assertThat(new IssuerKeyIdRule(kid).checkRule(issuer(iss), null)).isFailed()
                .detail().contains("exceeds the maximum length");
    }
}
