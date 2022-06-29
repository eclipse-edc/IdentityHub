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

import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * Interfaces methods supported by a Web Node
 * See: <a href="https://identity.foundation/decentralized-web-node/spec/#interfaces">Web node interface identity foundation documentation.</a>
 */
public enum WebNodeInterfaceMethod {
    COLLECTIONS_QUERY("CollectionsQuery"),
    COLLECTIONS_WRITE("CollectionsWrite"),
    FEATURE_DETECTION_READ("FeatureDetectionRead"),
    INVALID_METHOD("InvalidMethod");

    private String name;

    WebNodeInterfaceMethod(String name) {
        this.name = name;
    }

    @NotNull
    public static WebNodeInterfaceMethod fromName(String name) {
        return Stream.of(values())
                .filter(v -> v.name.equals(name))
                .findFirst()
                .orElse(INVALID_METHOD);
    }

    public String getName() {
        return name;
    }
}
