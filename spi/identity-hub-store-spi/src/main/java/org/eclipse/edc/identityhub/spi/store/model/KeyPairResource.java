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

package org.eclipse.edc.identityhub.spi.store.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.edc.identityhub.spi.model.ParticipantResource;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.spi.security.KeyParserRegistry;
import org.eclipse.edc.spi.security.Vault;

import java.time.Duration;

/**
 * A {@link KeyPairResource} contains key material for a particular {@link ParticipantContext}. The public key is stored in the database in serialized form (JWK or PEM) and the private
 * key is referenced via an alias, it is actually stored in a {@link Vault}.
 */
public class KeyPairResource extends ParticipantResource {
    private String id;
    private long timestamp;
    private String keyId;
    private String groupName;
    private boolean isDefaultPair;
    private long useDuration;
    private long rotationDuration;
    private String serializedPublicKey;
    private String privateKeyAlias;
    private int state;

    private KeyPairResource() {
    }

    public String getGroupName() {
        return groupName;
    }

    /**
     * The database ID of this KeyPairResource.
     */
    public String getId() {
        return id;
    }

    /**
     * Whether this KeyPair is the default for a {@link ParticipantContext}.
     */
    public boolean isDefaultPair() {
        return isDefaultPair;
    }

    /**
     * A String that should be used when referencing this key material in e.g. a JWT.
     */
    public String getKeyId() {
        return keyId;
    }


    /**
     * The alias under which the private key is stored in the vault.
     */
    public String getPrivateKeyAlias() {
        return privateKeyAlias;
    }

    /**
     * The overall duration this key is still in service (used for verification) after a rotation has been started.
     */
    public long getRotationDuration() {
        return rotationDuration;
    }

    /**
     * The public key in JWK or PEM format. Consider using a {@link KeyParserRegistry} to restore the key.
     */
    public String getSerializedPublicKey() {
        return serializedPublicKey;
    }

    /**
     * The Epoch Millis when this KeyPair was created.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Maximum time (starting from {@link KeyPairResource#getTimestamp()}) this key should be in use.
     */
    public long getUseDuration() {
        return useDuration;
    }


    public int getState() {
        return state;
    }

    @JsonIgnore
    public void rotate(long duration) {
        state = KeyPairState.ROTATED.code();
        rotationDuration = duration;
        isDefaultPair = false;
    }

    public void revoke() {
        state = KeyPairState.REVOKED.code();
        isDefaultPair = false;
    }

    public static final class Builder extends ParticipantResource.Builder<KeyPairResource, KeyPairResource.Builder> {

        private Builder() {
            super(new KeyPairResource());
        }

        public Builder groupName(String groupName) {
            entity.groupName = groupName;
            return this;
        }

        public Builder id(String id) {
            entity.id = id;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        public Builder timestamp(long timestamp) {
            entity.timestamp = timestamp;
            return this;
        }

        public Builder keyId(String keyId) {
            entity.keyId = keyId;
            return this;
        }

        public Builder isDefaultPair(boolean isDefaultPair) {
            entity.isDefaultPair = isDefaultPair;
            return this;
        }

        public Builder useDuration(long useDuration) {
            entity.useDuration = useDuration;
            return this;
        }

        public Builder rotationDuration(long rotationDuration) {
            entity.rotationDuration = rotationDuration;
            return this;
        }

        public Builder serializedPublicKey(String serializedPublicKey) {
            entity.serializedPublicKey = serializedPublicKey;
            return this;
        }

        public Builder privateKeyAlias(String privateKeyAlias) {
            entity.privateKeyAlias = privateKeyAlias;
            return this;
        }

        public Builder state(int state) {
            entity.state = state;
            return this;
        }

        public Builder state(KeyPairState state) {
            entity.state = state.code();
            return this;
        }

        public KeyPairResource build() {
            if (entity.useDuration == 0) {
                entity.useDuration = Duration.ofDays(6 * 30).toMillis();
            }
            return super.build();
        }

        public static Builder newInstance() {
            return new Builder();
        }
    }
}
