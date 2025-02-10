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

package org.eclipse.edc.issuerservice.issuance;

import org.eclipse.edc.issuerservice.issuance.attestation.AttestationDefinitionServiceImpl;
import org.eclipse.edc.issuerservice.issuance.credentialdefinition.CredentialDefinitionServiceImpl;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.store.CredentialDefinitionStore;
import org.eclipse.edc.issuerservice.spi.participant.store.ParticipantStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static org.eclipse.edc.issuerservice.issuance.IssuanceServicesExtension.NAME;

@Extension(value = NAME)
public class IssuanceServicesExtension implements ServiceExtension {
    public static final String NAME = "IssuerService Issuance Services Extension";
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private CredentialDefinitionStore store;

    @Inject
    private AttestationDefinitionStore attestationDefinitionStore;
    @Inject
    private ParticipantStore participantStore;

    @Provider
    public CredentialDefinitionService createParticipantService() {
        return new CredentialDefinitionServiceImpl(transactionContext, store, attestationDefinitionStore);
    }

    @Provider
    public AttestationDefinitionService createAttestationService() {
        return new AttestationDefinitionServiceImpl(transactionContext, attestationDefinitionStore, participantStore);
    }
}
