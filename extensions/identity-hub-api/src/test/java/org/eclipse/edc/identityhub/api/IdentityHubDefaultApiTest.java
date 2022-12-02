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

package org.eclipse.edc.identityhub.api;

import org.eclipse.edc.junit.extensions.EdcExtension;
import org.junit.jupiter.api.BeforeAll;

import java.util.Map;

import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;

public class IdentityHubDefaultApiTest extends IdentityHubApiTest {
    private static int port;

    @BeforeAll
    static void prepare() {
        port = getFreePort();
    }

    @Override
    protected String configureApi(EdcExtension extension) {
        extension.setConfiguration(Map.of("web.http.port", String.valueOf(port)));
        return String.format("http://localhost:%s/api", port);
    }
}
