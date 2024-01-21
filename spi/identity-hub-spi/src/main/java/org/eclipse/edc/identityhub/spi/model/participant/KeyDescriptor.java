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

package org.eclipse.edc.identityhub.spi.model.participant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.security.Vault;

import java.util.Map;

/**
 * Container object to describe, what security keys should be used when creating a {@link ParticipantContext}.
 * There are two basic options:
 * <ol>
 *     <li>Keys already exist - the public key can be specified using PEM or JWK format. The private key is expected to exist in the {@link Vault} under the alias {@link KeyDescriptor#getPrivateKeyAlias()}</li>
 *     <li>Keys don't exist - keys are to be generated, in which case the the {@link KeyDescriptor#getKeyGeneratorParams()} have to be specified.</li>
 * </ol>
 * Specifying both options - or neither - is an error.
 */
@JsonDeserialize(builder = KeyDescriptor.Builder.class)
public class KeyDescriptor {
    private String keyId;
    private String privateKeyAlias;
    private Map<String, Object> publicKeyJwk;
    private String publicKeyPem;
    private Map<String, Object> keyGeneratorParams;

    private KeyDescriptor() {
    }

    public String getKeyId() {
        return keyId;
    }

    public String getPrivateKeyAlias() {
        return privateKeyAlias;
    }

    public Map<String, Object> getPublicKeyJwk() {
        return publicKeyJwk;
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public Map<String, Object> getKeyGeneratorParams() {
        return keyGeneratorParams;
    }


    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final KeyDescriptor keyDescriptor;

        private Builder() {
            keyDescriptor = new KeyDescriptor();
        }

        public Builder keyId(String keyId) {
            keyDescriptor.keyId = keyId;
            return this;
        }

        public Builder privateKeyAlias(String privateKeyAlias) {
            keyDescriptor.privateKeyAlias = privateKeyAlias;
            return this;
        }

        public Builder publicKeyJwk(Map<String, Object> publicKeyJwk) {
            keyDescriptor.publicKeyJwk = publicKeyJwk;
            return this;
        }

        public Builder publicKeyPem(String publicKeyPem) {
            keyDescriptor.publicKeyPem = publicKeyPem;
            return this;
        }

        public Builder keyGeneratorParams(Map<String, Object> keyGeneratorParams) {
            keyDescriptor.keyGeneratorParams = keyGeneratorParams;
            return this;
        }

        public KeyDescriptor build() {
            return keyDescriptor;
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }
    }
}
