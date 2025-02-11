/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.defaults.store;

import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.store.AttestationDefinitionStoreTestBase;

class InMemoryAttestationDefinitionStoreTest extends AttestationDefinitionStoreTestBase {

    private final InMemoryAttestationDefinitionStore store = new InMemoryAttestationDefinitionStore();

    @Override
    protected AttestationDefinitionStore getStore() {
        return store;
    }
}