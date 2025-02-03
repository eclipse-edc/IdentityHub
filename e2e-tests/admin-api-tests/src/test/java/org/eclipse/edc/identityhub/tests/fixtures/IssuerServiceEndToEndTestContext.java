/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.tests.fixtures;

import org.eclipse.edc.junit.extensions.EmbeddedRuntime;

/**
 * IssuerService end to end context used in tests extended with {@link IssuerServiceEndToEndExtension}
 */
public class IssuerServiceEndToEndTestContext {

    public static final String SUPER_USER = "super-user";

    private final EmbeddedRuntime runtime;
    private final IssuerServiceRuntimeConfiguration configuration;

    public IssuerServiceEndToEndTestContext(EmbeddedRuntime runtime, IssuerServiceRuntimeConfiguration configuration) {
        this.runtime = runtime;
        this.configuration = configuration;
    }

    public EmbeddedRuntime getRuntime() {
        return runtime;
    }

    public IssuerServiceRuntimeConfiguration.Endpoint getAdminEndpoint() {
        return configuration.getAdminEndpoint();
    }
}
