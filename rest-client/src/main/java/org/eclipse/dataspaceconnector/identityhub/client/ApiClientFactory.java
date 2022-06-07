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

package org.eclipse.dataspaceconnector.identityhub.client;

/**
 * Factory class for {@link ApiClient}.
 */
public class ApiClientFactory {
    private ApiClientFactory() {
    }

    /**
     * Create a new instance of {@link ApiClient} configured to access the given URL.
     *
     * @param baseUri API base URL.
     * @return a new instance of {@link ApiClient}.
     */
    public static ApiClient createApiClient(String baseUri) {
        var apiClient = new ApiClient();
        apiClient.updateBaseUri(baseUri);
        return apiClient;
    }
}
