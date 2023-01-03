/*
 *  Copyright (c) 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.store.cosmos.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.edc.azure.cosmos.CosmosDocument;
import org.eclipse.edc.identityhub.store.spi.IdentityHubRecord;


@JsonTypeName("dataspaceconnector:identityhubrecorddocument")
public class IdentityHubRecordDocument extends CosmosDocument<IdentityHubRecord> {

    @JsonCreator
    public IdentityHubRecordDocument(@JsonProperty("wrappedInstance") IdentityHubRecord record,
                                     @JsonProperty("partitionKey") String partitionKey) {
        super(record, partitionKey);
    }

    @Override
    public String getId() {
        return getWrappedInstance().getId();
    }
}
