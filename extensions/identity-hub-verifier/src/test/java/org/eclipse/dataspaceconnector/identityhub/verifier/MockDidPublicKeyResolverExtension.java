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

package org.eclipse.dataspaceconnector.identityhub.mock;

import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;

import static org.mockito.Mockito.mock;

/**
 * Extension to provide a mock implementation of {@link DidPublicKeyResolver} for testing.
 */
public class MockDidPublicKeyResolverExtension implements ServiceExtension {

    @Provider
    public DidPublicKeyResolver didPublicKeyResolver() {
        return mock(DidPublicKeyResolver.class);
    }
}
