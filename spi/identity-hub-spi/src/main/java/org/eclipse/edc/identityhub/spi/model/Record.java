/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 * The {@link Record} is an object produced by CollectionQuery interface
 */
@JsonDeserialize(builder = Record.Builder.class)
public class Record {

    private String id;
    private byte[] data;

    private String dataFormat;

    private long createdAt;

    private Record() {

    }

    public byte[] getData() {
        return data;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getDataFormat() {
        return dataFormat;
    }

    public String getId() {
        return id;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private final Record record;

        private Builder() {
            record = new Record();
        }

        @JsonCreator()
        public static Record.Builder newInstance() {
            return new Record.Builder();
        }

        public Builder id(String id) {
            record.id = id;
            return this;
        }

        public Builder data(byte[] data) {
            record.data = data;
            return this;
        }

        public Builder dataFormat(String dataFormat) {
            record.dataFormat = dataFormat;
            return this;
        }

        public Builder createdAt(long createdAt) {
            record.createdAt = createdAt;
            return this;
        }

        public Record build() {
            Objects.requireNonNull(record.id, "RecordId cannot be null");
            return record;
        }
    }
}
