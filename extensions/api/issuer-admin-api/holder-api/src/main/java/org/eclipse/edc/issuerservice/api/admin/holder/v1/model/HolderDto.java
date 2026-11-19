/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.issuerservice.api.admin.holder.v1.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;

import java.util.Map;

public record HolderDto(@JsonProperty(value = "holderId", required = true) String id,
                        @JsonProperty(value = "did", required = true) String did,
                        @JsonProperty("name") String name,
                        @JsonProperty("properties") Map<String, Object> properties) {

    public static HolderDto from(Holder holder) {
        return new HolderDto(holder.getHolderId(), holder.getDid(), holder.getHolderName(), holder.getProperties());
    }

    public Holder toHolder(String participantContextId) {
        return Holder.Builder.newInstance()
                .holderId(id)
                .did(did)
                .holderName(name)
                .participantContextId(participantContextId)
                .properties(properties)
                .build();
    }
}


