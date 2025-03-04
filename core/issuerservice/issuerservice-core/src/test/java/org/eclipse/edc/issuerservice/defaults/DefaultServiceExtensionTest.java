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

package org.eclipse.edc.issuerservice.defaults;

import org.eclipse.edc.issuerservice.defaults.store.InMemoryAttestationDefinitionStore;
import org.eclipse.edc.issuerservice.defaults.store.InMemoryCredentialDefinitionStore;
import org.eclipse.edc.issuerservice.defaults.store.InMemoryHolderStore;
import org.eclipse.edc.issuerservice.defaults.store.InMemoryIssuanceProcessStore;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DependencyInjectionExtension.class)
class DefaultServiceExtensionTest {
    @Test
    void verifyDefaultServices(DefaultServiceExtension extension) {
        assertThat(extension.createInMemoryParticipantStore()).isInstanceOf(InMemoryHolderStore.class);
        assertThat(extension.createInMemoryCredentialDefinitionStore()).isInstanceOf(InMemoryCredentialDefinitionStore.class);
        assertThat(extension.createInMemoryAttestationStore()).isInstanceOf(InMemoryAttestationDefinitionStore.class);
        assertThat(extension.createIssuanceProcessStore()).isInstanceOf(InMemoryIssuanceProcessStore.class);
    }

}