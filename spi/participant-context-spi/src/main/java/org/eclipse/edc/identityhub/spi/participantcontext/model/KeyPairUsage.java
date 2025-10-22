/*
 *  Copyright (c) 2025 Metaform Systems Inc.
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

package org.eclipse.edc.identityhub.spi.participantcontext.model;

public enum KeyPairUsage {
    CREDENTIAL_SIGNING("credential", 100),
    PRESENTATION_SIGNING("presentation", 200),
    ACCESS_TOKEN("access_token", 300),
    ID_TOKEN("id_token", 400);

    private final String name;
    private final int code;

    KeyPairUsage(String keyPairName, int code) {
        this.name = keyPairName;
        this.code = code;
    }

    public static KeyPairUsage fromName(String s) {
        return KeyPairUsage.valueOf(KeyPairUsage.class, s);
    }

    @Override
    public String toString() {
        return name;
    }

    public int code() {
        return code;
    }
}
