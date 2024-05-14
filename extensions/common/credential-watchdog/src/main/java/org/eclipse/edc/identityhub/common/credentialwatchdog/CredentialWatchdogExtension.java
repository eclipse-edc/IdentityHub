/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.common.credentialwatchdog;

import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialStatusCheckService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.security.SecureRandom;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.eclipse.edc.identityhub.common.credentialwatchdog.CredentialWatchdogExtension.NAME;

@Extension(value = NAME)
public class CredentialWatchdogExtension implements ServiceExtension {
    public static final String NAME = "VerifiableCredential Watchdog Extension";

    public static final int DEFAULT_WATCHDOG_PERIOD = 60;
    @Setting(value = "Period (in seconds) at which the Watchdog thread checks all stored credentials for their status. Configuring a number <=0 disables the Watchdog.",
            type = "integer", min = 0, defaultValue = DEFAULT_WATCHDOG_PERIOD + "")
    public static final String WATCHDOG_PERIOD_PROPERTY = "edc.iam.credential.status.check.period";

    public static final int DEFAULT_WATCHDOG_INITIAL_DELAY = 5;
    @Setting(value = "Initial delay (in seconds) before the Watchdog thread begins its work.",
            type = "integer", min = 0, defaultValue = "random number [1.." + DEFAULT_WATCHDOG_INITIAL_DELAY + "]")
    public static final String WATCHDOG_DELAY_PROPERTY = "edc.iam.credential.status.check.delay";
    public static final String CREDENTIAL_WATCHDOG = "CredentialWatchdog";
    private final SecureRandom random = new SecureRandom();
    @Inject
    private ExecutorInstrumentation executorInstrumentation;
    @Inject
    private CredentialStatusCheckService credentialStatusCheckService;
    @Inject
    private CredentialStore credentialStore;
    @Inject
    private TransactionContext transactionContext;
    private ScheduledExecutorService scheduledExecutorService;
    private Integer watchdogPeriod;
    private Monitor monitor;
    private int initialDelay;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        watchdogPeriod = context.getSetting(WATCHDOG_PERIOD_PROPERTY, DEFAULT_WATCHDOG_PERIOD);
        monitor = context.getMonitor().withPrefix(CREDENTIAL_WATCHDOG);

        if (watchdogPeriod <= 0) {
            monitor.debug(() -> "Config property '%s' was <= 0 (%d). The Credential Watchdog is disabled.".formatted(WATCHDOG_PERIOD_PROPERTY, watchdogPeriod));
        } else {
            initialDelay = context.getSetting(WATCHDOG_DELAY_PROPERTY, randomDelay());
            monitor.debug(() -> "Credential watchdog will run with a delay of %d seconds, at an interval of %d seconds".formatted(initialDelay, watchdogPeriod));
            scheduledExecutorService = executorInstrumentation.instrument(Executors.newSingleThreadScheduledExecutor(), CREDENTIAL_WATCHDOG);
        }
    }

    @Override
    public void start() {
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            monitor.debug(() -> "Starting credential watchdog in %d seconds, every %d seconds".formatted(initialDelay, watchdogPeriod));
            scheduledExecutorService.scheduleAtFixedRate(new CredentialWatchdog(credentialStore, credentialStatusCheckService, monitor, transactionContext), initialDelay, watchdogPeriod, TimeUnit.SECONDS);
        }
    }

    @Override
    public void shutdown() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
        }
    }

    private Integer randomDelay() {
        return random.nextInt(1, DEFAULT_WATCHDOG_INITIAL_DELAY);
    }
}
