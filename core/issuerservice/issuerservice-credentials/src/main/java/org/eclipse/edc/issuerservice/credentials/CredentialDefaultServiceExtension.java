/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.credentials;

import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerProfile;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.issuerservice.credentials.statuslist.StatusListInfoFactoryRegistryImpl;
import org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringStatusListConfiguration;
import org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringStatusListManager;
import org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.StatusListType;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListCredentialPublisher;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListInfoFactoryRegistry;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListManager;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Extension(CredentialDefaultServiceExtension.NAME)
public class CredentialDefaultServiceExtension implements ServiceExtension {

    public static final String NAME = "Issuer Service Credential Default Services";

    @Configuration
    private BitstringStatusListConfiguration statusListConfiguration;

    @Inject
    private StatusListCredentialPublisher credentialPublisher;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private CredentialStore store;
    @Inject
    private CredentialGeneratorRegistry registry;
    @Inject
    private IdentityHubParticipantContextService participantContextService;
    @Inject
    private Clock clock;
    @Inject
    private ExecutorInstrumentation executorInstrumentation;
    @Inject
    private Monitor monitor;
    @Inject
    private DcpIssuerProfile issuerProfile;

    private BitstringStatusListManager statusListManager;
    private ScheduledExecutorService renewalExecutor;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public StatusListInfoFactoryRegistry statusListInfoFactoryRegistry() {
        return new StatusListInfoFactoryRegistryImpl();
    }

    /**
     * The status list manager for the status list type of the issuer's DCP profile. The runtime does not start with a
     * profile whose status list type it cannot handle: such a profile needs another status list manager.
     */
    @Provider(isDefault = true)
    public StatusListManager statusListManager() {
        if (statusListManager == null) {
            var profile = issuerProfile.get();
            var type = StatusListType.forEntryType(profile.statusListType())
                    .orElseThrow(() -> new EdcException("The status list type '%s' of the DCP profile '%s' is not supported, it needs another StatusListManager"
                            .formatted(profile.statusListType(), profile.name())));
            // replaces the values that cannot be used as they are, and warns about them
            statusListConfiguration = statusListConfiguration.validated(monitor);
            statusListManager = new BitstringStatusListManager(type, store, transactionContext, registry, participantContextService, credentialPublisher,
                    statusListConfiguration, clock, monitor);
        }
        return statusListManager;
    }

    /**
     * Renews the status list credentials that are about to expire periodically, so that they do not expire while nothing
     * else renews them. Only those of the default {@link StatusListManager} are renewed, another one takes
     * care of its own.
     */
    @Override
    public void start() {
        if (statusListManager == null) {
            return;
        }
        var renewalPeriodSeconds = statusListConfiguration.renewalPeriodSeconds();
        if (renewalPeriodSeconds <= 0) {
            monitor.warning("The periodic renewal of status list credentials is deactivated, so they expire. An expired one is only renewed when a credential on it is revoked, or when a new credential is added to it");
            return;
        }
        var threshold = statusListConfiguration.renewalThreshold();
        renewalExecutor = executorInstrumentation.instrument(Executors.newSingleThreadScheduledExecutor(), "Status List Renewal");
        renewalExecutor.scheduleAtFixedRate(() -> renewExpiring(threshold), 0, renewalPeriodSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void shutdown() {
        if (renewalExecutor != null) {
            renewalExecutor.shutdownNow();
        }
    }

    private void renewExpiring(Duration threshold) {
        // an exception would end the periodic execution
        try {
            statusListManager.renewExpiring(threshold);
        } catch (RuntimeException e) {
            monitor.warning("Could not renew the status list credentials that are about to expire", e);
        }
    }
}
