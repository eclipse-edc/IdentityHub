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

package org.eclipse.edc.identityhub.spi.participantcontext.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.iam.did.spi.document.DidConstants;
import org.eclipse.edc.spi.security.Vault;

import java.util.Map;
import java.util.UUID;

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
    private String resourceId = UUID.randomUUID().toString();
    private String keyId;
    private String type;
    private String privateKeyAlias;
    private Map<String, Object> publicKeyJwk;
    private String publicKeyPem;
    private Map<String, Object> keyGeneratorParams;
    private boolean isActive = true;

    private KeyDescriptor() {
    }

    /**
     * The ID of the key. Will be used to reference the key that was used for signing, e.g. as "kid" header in JSON Web Tokens
     */
    public String getKeyId() {
        return keyId;
    }

    /**
     * The type of the verification method associated with this key.
     * It specifies in which cryptographic context the key will be used.
     */
    public String getType() {
        return type;
    }

    /**
     * Alias under which the private key is stored in the vault. If keys are to be generated, the new private key will get stored
     * under the that alias.
     */
    public String getPrivateKeyAlias() {
        return privateKeyAlias;
    }

    /**
     * Public key in JWK format (JSON string). If this is specified, {@link KeyDescriptor#getPublicKeyPem()} and {@link KeyDescriptor#getKeyGeneratorParams()} MUST be null.
     */
    public Map<String, Object> getPublicKeyJwk() {
        return publicKeyJwk;
    }

    /**
     * Public key in PEM format. If this is specified, {@link KeyDescriptor#getPublicKeyJwk()} ()} and {@link KeyDescriptor#getKeyGeneratorParams()} MUST be null.
     */
    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    /**
     * Specify only if keys are to be generated. Must contain an "algorithm -> [EC | RSA | EdDSA]" entry, possibly a "curve" parameter. If specified, {@link KeyDescriptor#getPublicKeyPem()} and {@link KeyDescriptor#getPublicKeyJwk()}
     * MUST be null.
     */
    public Map<String, Object> getKeyGeneratorParams() {
        return keyGeneratorParams;
    }

    /**
     * Determines whether the new key should be set to {@code KeyPairState.ACTIVE}.
     * If this is set to {@code false}, the key pair will be created in the {@code KeyPairState.CREATED} state.
     * Defaults to {@code true}.
     */
    public boolean isActive() {
        return isActive;
    }

    public String getResourceId() {
        return resourceId;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final KeyDescriptor keyDescriptor;

        private Builder() {
            keyDescriptor = new KeyDescriptor();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder keyId(String keyId) {
            keyDescriptor.keyId = keyId;
            return this;
        }

        public Builder type(String type) {
            keyDescriptor.type = type;
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

        public Builder active(boolean isActive) {
            keyDescriptor.isActive = isActive;
            return this;
        }

        public Builder resourceId(String resourceId) {
            keyDescriptor.resourceId = resourceId;
            return this;
        }

        public KeyDescriptor build() {
            if (keyDescriptor.type == null) {
                keyDescriptor.type = DidConstants.JSON_WEB_KEY_2020;
            }
            return keyDescriptor;
        }
    }
}
