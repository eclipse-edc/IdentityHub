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

import com.fasterxml.jackson.annotation.JsonProperty;

public enum KeyPairUsage {
    @JsonProperty("sign_credentials")
    CREDENTIAL_SIGNING("sign_credentials", 100),
    @JsonProperty("sign_presentation")
    PRESENTATION_SIGNING("sign_presentation", 200),
    @JsonProperty("sign_token")
    TOKEN_SIGNING("sign_token", 300);

    private final String value;
    private final int code;

    KeyPairUsage(String keyPairName, int code) {
        this.value = keyPairName;
        this.code = code;
    }

    @Override
    public String toString() {
        return value;
    }

    public int code() {
        return code;
    }
}
