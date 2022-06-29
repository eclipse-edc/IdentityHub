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

package org.eclipse.dataspaceconnector.identityhub.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Feature Detection object produced by a compliant decentralized Web Node.
 * See: <a href="https://identity.foundation/decentralized-web-node/spec/#feature-detection">Feature detection identity foundation documentation.</a>
 */
@JsonDeserialize(builder = FeatureDetection.Builder.class)
public class FeatureDetection {

    private static final String TYPE = "FeatureDetection";
    private WebNodeInterfaces interfaces;

    public String getType() {
        return TYPE;
    }

    public WebNodeInterfaces getInterfaces() {
        return interfaces;
    }

    private FeatureDetection() {
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final FeatureDetection featureDetection;

        public Builder() {
            featureDetection = new FeatureDetection();
        }

        @JsonCreator()
        public static FeatureDetection.Builder newInstance() {
            return new FeatureDetection.Builder();
        }

        public FeatureDetection.Builder interfaces(WebNodeInterfaces interfaces) {
            featureDetection.interfaces = interfaces;
            return this;
        }

        public FeatureDetection build() {
            return featureDetection;
        }

    }
}
