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

package org.eclipse.edc.issuerservice.issuance.generator;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a {@link CredentialSubject} as the claims of a signed credential.
 */
final class CredentialSubjectClaims {

    private static final String ID = "id";

    private CredentialSubjectClaims() {
    }

    /**
     * The claims of the credential subject, including its ID. A generated credential subject carries its ID also as one
     * of its claims, but once it has been serialized and deserialized, e.g. by a store, the ID is only kept as the
     * subject's own property. Without adding it back, a credential signed from it would not identify its holder.
     *
     * @param subject the credential subject
     * @return the claims of the subject, with the subject's ID unless the claims contain one already
     */
    static Map<String, Object> of(CredentialSubject subject) {
        var claims = new HashMap<>(subject.getClaims());
        if (subject.getId() != null) {
            claims.putIfAbsent(ID, subject.getId());
        }
        return claims;
    }
}
