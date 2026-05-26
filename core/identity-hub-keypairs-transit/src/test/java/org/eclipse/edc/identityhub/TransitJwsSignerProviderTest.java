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

package org.eclipse.edc.identityhub;

import org.eclipse.edc.identityhub.transit.TransitEngine;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;

class TransitJwsSignerProviderTest {
    private final TransitEngine transitEngine = mock();
    private final TransitJwsSignerProvider provider = new TransitJwsSignerProvider(transitEngine);

    @Test
    void createJwsSigner() {
        assertThat(provider.createJwsSigner("test-key")).isSucceeded().isInstanceOf(TransitSigner.class);
        assertThat(provider.createJwsSigner("test-participant", "test-key")).isSucceeded().isInstanceOf(TransitSigner.class);
    }
}