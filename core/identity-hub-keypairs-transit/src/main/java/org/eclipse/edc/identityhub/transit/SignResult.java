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

package org.eclipse.edc.identityhub.transit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SignResult {

    @JsonProperty("data")
    private Data data;

    @JsonProperty("mount_type")
    private String mountType;

    private SignResult() {
    }

    public Data getData() {
        return data;
    }

    public String getMountType() {
        return mountType;
    }

    @JsonIgnore
    public String getSignature() {
        return getData().getSignature();
    }

    public static class Data {

        @JsonProperty("key_version")
        private int keyVersion;

        @JsonProperty("signature")
        private String signature;

        private Data() {
        }

        public int getKeyVersion() {
            return keyVersion;
        }

        public String getSignature() {
            return signature;
        }

    }
}