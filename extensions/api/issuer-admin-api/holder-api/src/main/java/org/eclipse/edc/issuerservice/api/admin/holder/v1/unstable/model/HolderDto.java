/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.api.admin.holder.v1.unstable.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;

public record HolderDto(@JsonProperty(value = "holderId", required = true) String id,
                        @JsonProperty(value = "did", required = true) String did,
                        @JsonProperty("name") String name) {

    public static HolderDto from(Holder holder) {
        return new HolderDto(holder.getHolderId(), holder.getDid(), holder.getHolderName());
    }

    public Holder toHolder(String participantContextId) {
        return Holder.Builder.newInstance().holderId(id).did(did).holderName(name).participantContextId(participantContextId).build();
    }
}


