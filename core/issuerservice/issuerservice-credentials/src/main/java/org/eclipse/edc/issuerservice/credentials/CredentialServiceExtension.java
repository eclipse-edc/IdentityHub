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

package org.eclipse.edc.issuerservice.credentials;

import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.decentralizedclaims.spi.CredentialServiceUrlResolver;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerMetadataService;
import org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.issuerservice.credentials.offers.IssuerCredentialOfferServiceImpl;
import org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringStatusListFactory;
import org.eclipse.edc.issuerservice.spi.credentials.CredentialStatusService;
import org.eclipse.edc.issuerservice.spi.credentials.IssuerCredentialOfferService;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListInfoFactoryRegistry;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListManager;
import org.eclipse.edc.issuerservice.spi.holder.store.HolderStore;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_1_0;
import static org.eclipse.edc.issuerservice.credentials.CredentialServiceExtension.NAME;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringConstants.BITSTRING_STATUS_LIST_ENTRY;

@Extension(value = NAME)
public class CredentialServiceExtension implements ServiceExtension {

    public static final String NAME = "Issuer Service Credential Service";

    @Inject
    private TransactionContext transactionContext;
    @Inject
    private CredentialStore store;
    @Inject
    private HolderStore holderStore;
    @Inject
    private CredentialServiceUrlResolver credentialServiceUrlResolver;
    @Inject
    private ParticipantSecureTokenService sts;
    @Inject
    private IdentityHubParticipantContextService participantContextService;
    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private CredentialGeneratorRegistry registry;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private DcpIssuerMetadataService issuerMetadataService;
    @Inject
    private StatusListInfoFactoryRegistry statusListInfoFactoryRegistry;
    @Inject
    private StatusListManager statusListManager;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public CredentialStatusService getStatusListService(ServiceExtensionContext context) {
        statusListInfoFactoryRegistry.register(BITSTRING_STATUS_LIST_ENTRY, new BitstringStatusListFactory(store));

        return new CredentialStatusServiceImpl(store, transactionContext, context.getMonitor(), statusListInfoFactoryRegistry, statusListManager, registry);
    }

    @Provider
    public IssuerCredentialOfferService credentialOfferService(ServiceExtensionContext context) {
        return new IssuerCredentialOfferServiceImpl(transactionContext, holderStore, credentialServiceUrlResolver, sts, participantContextService, httpClient, context.getMonitor(),
                transformerRegistry.forContext(DCP_SCOPE_V_1_0),
                issuerMetadataService);
    }

}
