/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.credential.offer.handler;

import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpProfileRegistry;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestManager;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.events.CredentialOfferReceived;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static org.eclipse.edc.identityhub.credential.offer.handler.CredentialOfferHandlerExtension.NAME;

@Extension(value = NAME)
public class CredentialOfferHandlerExtension implements ServiceExtension {
    public static final String NAME = "CredentialOfferHandlerExtension";

    @Inject
    private EventRouter eventRouter;
    @Inject
    private DcpProfileRegistry dcpProfileRegistry;
    @Inject
    private CredentialRequestManager credentialRequestManager;
    @Inject
    private CredentialOfferStore credentialOfferStore;
    @Inject
    private TransactionContext transactionContext;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var handler = new CredentialOfferHandler(context.getMonitor(), credentialRequestManager, dcpProfileRegistry, credentialOfferStore, transactionContext);
        eventRouter.registerSync(CredentialOfferReceived.class, handler);
    }

    @Override
    public String name() {
        return NAME;
    }
}
