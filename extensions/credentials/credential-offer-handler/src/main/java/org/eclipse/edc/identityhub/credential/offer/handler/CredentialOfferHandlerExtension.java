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
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestManager;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.events.CredentialOfferReceived;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.Arrays;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.eclipse.edc.identityhub.credential.offer.handler.CredentialOfferHandlerExtension.NAME;

@Extension(value = NAME)
public class CredentialOfferHandlerExtension implements ServiceExtension {
    public static final String NAME = "CredentialOfferHandlerExtension";
    static final String TRUSTED_ISSUERS_KEY = "edc.iam.credential.offer.trusted.issuers";

    @Setting(description = "Comma-separated list of issuer DIDs whose credential offers automatically trigger a credential request. " +
            "Offers of issuers the participant already holds credentials from do so as well. Offers of other issuers are only stored. " +
            "If not set, offers of any issuer trigger a credential request.",
            required = false, key = TRUSTED_ISSUERS_KEY)
    private String trustedIssuers;

    @Inject
    private EventRouter eventRouter;
    @Inject
    private DcpProfileRegistry dcpProfileRegistry;
    @Inject
    private CredentialRequestManager credentialRequestManager;
    @Inject
    private CredentialOfferStore credentialOfferStore;
    @Inject
    private HolderCredentialRequestStore holderCredentialRequestStore;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private CredentialStore credentialStore;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var trustedIssuerDids = parseTrustedIssuers();
        if (trustedIssuerDids.isEmpty()) {
            context.getMonitor().warning("No trusted issuers are configured ('%s'), so credential offers of any issuer automatically trigger a credential request."
                    .formatted(TRUSTED_ISSUERS_KEY));
        }
        var handler = new CredentialOfferHandler(context.getMonitor(), credentialRequestManager, dcpProfileRegistry, credentialOfferStore, holderCredentialRequestStore, transactionContext,
                credentialStore, trustedIssuerDids);
        eventRouter.registerSync(CredentialOfferReceived.class, handler);
    }

    private Set<String> parseTrustedIssuers() {
        if (trustedIssuers == null) {
            return Set.of();
        }
        return Arrays.stream(trustedIssuers.split(","))
                .map(String::trim)
                .filter(did -> !did.isEmpty())
                .collect(toSet());
    }

    @Override
    public String name() {
        return NAME;
    }
}
