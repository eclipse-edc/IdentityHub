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

package org.eclipse.dataspaceconnector.identityhub.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Interfaces supported by a Web Node
 * See: <a href="https://identity.foundation/decentralized-web-node/spec/#interfaces">Web node interface identity foundation documentation.</a>
 */
@JsonDeserialize(builder = WebNodeInterfaces.Builder.class)
public class WebNodeInterfaces {

    private final Map<String, Boolean> collections = new HashMap<>();
    // not supported interfaces ATM:
    private final Map<String, Boolean> actions = new HashMap<>();
    private final Map<String, Boolean> permissions = new HashMap<>();
    private final Map<String, Boolean> messaging = new HashMap<>();

    private WebNodeInterfaces() {
    }

    public Map<String, Boolean> getCollections() {
        return collections;
    }

    public Map<String, Boolean> getActions() {
        return actions;
    }

    public Map<String, Boolean> getPermissions() {
        return permissions;
    }

    public Map<String, Boolean> getMessaging() {
        return messaging;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final WebNodeInterfaces webNodeInterfaces;

        public Builder() {
            webNodeInterfaces = new WebNodeInterfaces();
        }

        @JsonCreator()
        public static WebNodeInterfaces.Builder newInstance() {
            return new WebNodeInterfaces.Builder();
        }

        public WebNodeInterfaces.Builder supportedCollection(String collection) {
            webNodeInterfaces.collections.put(collection, true);
            return this;
        }

        public WebNodeInterfaces build() {
            return webNodeInterfaces;
        }

    }
}
