/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.identitytrust.sts.spi.model;

import org.eclipse.edc.spi.entity.Entity;
import org.eclipse.edc.spi.security.Vault;

import java.util.Objects;

/**
 * The {@link StsAccount} contains information about STS clients.
 */
public class StsAccount extends Entity {
    private String clientId;
    private String did;
    private String name;
    private String secretAlias;

    private StsAccount() {
    }

    /**
     * The alias of the {@link StsAccount} secret stored in the {@link Vault}
     *
     * @return The secret alias
     */
    public String getSecretAlias() {
        return secretAlias;
    }

    /**
     * The name of the {@link StsAccount}
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * The client_id of the {@link StsAccount}
     *
     * @return The clientId
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * The client DID
     *
     * @return The DID
     */
    public String getDid() {
        return did;
    }


    public void updateSecretAlias(String secretAlias) {
        this.secretAlias = secretAlias;
    }

    public static class Builder extends Entity.Builder<StsAccount, Builder> {


        private Builder() {
            super(new StsAccount());
        }

        public static Builder newInstance() {
            return new Builder();
        }


        public Builder id(String id) {
            entity.id = id;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public StsAccount build() {
            Objects.requireNonNull(entity.id, "Client id");
            Objects.requireNonNull(entity.clientId, "Client client_id");
            Objects.requireNonNull(entity.name, "Client name");
            Objects.requireNonNull(entity.did, "Client DID");
            Objects.requireNonNull(entity.secretAlias, "Client secret alias");
            return super.build();
        }

        public Builder clientId(String clientId) {
            entity.clientId = clientId;
            return this;
        }

        public Builder name(String name) {
            entity.name = name;
            return this;
        }

        public Builder did(String did) {
            entity.did = did;
            return this;
        }

        public Builder secretAlias(String secretAlias) {
            entity.secretAlias = secretAlias;
            return this;
        }


    }
}
