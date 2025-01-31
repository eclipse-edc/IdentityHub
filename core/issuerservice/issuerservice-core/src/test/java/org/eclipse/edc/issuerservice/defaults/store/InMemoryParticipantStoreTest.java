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

import org.eclipse.edc.issuerservice.spi.participant.store.ParticipantStore;
import org.eclipse.edc.issuerservice.spi.participant.store.ParticipantStoreTestBase;

class InMemoryParticipantStoreTest extends ParticipantStoreTestBase {

    private final InMemoryParticipantStore store = new InMemoryParticipantStore();

    @Override
    protected ParticipantStore getStore() {
        return store;
    }
}