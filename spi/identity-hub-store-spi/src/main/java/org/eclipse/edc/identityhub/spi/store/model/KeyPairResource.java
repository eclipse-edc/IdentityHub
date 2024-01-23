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

import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.spi.security.KeyParserRegistry;
import org.eclipse.edc.spi.security.Vault;

/**
 * A {@link KeyPairResource} contains key material for a particular {@link ParticipantContext}. The public key is stored in the database in serialized form (JWK or PEM) and the private
 * key is referenced via an alias, it is actually stored in a {@link Vault}.
 */
public class KeyPairResource {
    private String id;
    private String participantId;
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
     * The {@link ParticipantContext} that this KeyPair belongs to.
     */
    public String getParticipantId() {
        return participantId;
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

    public static final class Builder {
        private final KeyPairResource keyPairResource;

        private Builder() {
            keyPairResource = new KeyPairResource();
        }

        public Builder groupName(String groupName) {
            keyPairResource.groupName = groupName;
            return this;
        }

        public Builder id(String id) {
            keyPairResource.id = id;
            return this;
        }

        public Builder participantId(String participantId) {
            keyPairResource.participantId = participantId;
            return this;
        }

        public Builder timestamp(long timestamp) {
            keyPairResource.timestamp = timestamp;
            return this;
        }

        public Builder keyId(String keyId) {
            keyPairResource.keyId = keyId;
            return this;
        }

        public Builder isDefaultPair(boolean isDefaultPair) {
            keyPairResource.isDefaultPair = isDefaultPair;
            return this;
        }

        public Builder useDuration(long useDuration) {
            keyPairResource.useDuration = useDuration;
            return this;
        }

        public Builder rotationDuration(long rotationDuration) {
            keyPairResource.rotationDuration = rotationDuration;
            return this;
        }

        public Builder serializedPublicKey(String serializedPublicKey) {
            keyPairResource.serializedPublicKey = serializedPublicKey;
            return this;
        }

        public Builder privateKeyAlias(String privateKeyAlias) {
            keyPairResource.privateKeyAlias = privateKeyAlias;
            return this;
        }

        public Builder state(int state) {
            keyPairResource.state = state;
            return this;
        }

        public Builder state(KeyPairState state) {
            keyPairResource.state = state.code();
            return this;
        }

        public KeyPairResource build() {

            return keyPairResource;
        }

        public static Builder newInstance() {
            return new Builder();
        }
    }
}
