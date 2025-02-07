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

import org.eclipse.edc.identityhub.spi.issuance.credentials.attestation.AttestationDefinitionStore;
import org.eclipse.edc.issuerservice.defaults.store.InMemoryAttestationDefinitionStore;
import org.eclipse.edc.issuerservice.defaults.store.InMemoryCredentialDefinitionStore;
import org.eclipse.edc.issuerservice.defaults.store.InMemoryParticipantStore;
import org.eclipse.edc.issuerservice.spi.credentialdefinition.store.CredentialDefinitionStore;
import org.eclipse.edc.issuerservice.spi.participant.store.ParticipantStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;

import static org.eclipse.edc.issuerservice.defaults.DefaultServiceExtension.NAME;

@Extension(value = NAME)
public class DefaultServiceExtension implements ServiceExtension {
    public static final String NAME = "IssuerService Default Services Extension";

    @Provider(isDefault = true)
    public ParticipantStore createInMemoryParticipantStore() {
        return new InMemoryParticipantStore();
    }


    @Provider(isDefault = true)
    public AttestationDefinitionStore createInMemoryAttestationStore() {
        return new InMemoryAttestationDefinitionStore();
    }

    @Provider(isDefault = true)
    public CredentialDefinitionStore createInMemoryCredentialDefinitionStore() {
        return new InMemoryCredentialDefinitionStore();
    }
}
