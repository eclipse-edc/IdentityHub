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
public class VerifyResult {

    @JsonProperty("data")
    private Data data;

    public VerifyResult() {
    }

    public Data getData() {
        return data;
    }

    @JsonIgnore
    public boolean isValid() {
        return data != null && data.isValid();
    }

    public static class Data {

        @JsonProperty("valid")
        private boolean valid;

        public Data() {
        }

        public boolean isValid() {
            return valid;
        }
    }
}