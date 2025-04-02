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

import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestManager;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialStatusCheckService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.identityhub.common.credentialwatchdog.CredentialWatchdogExtension.NAME;

@Extension(value = NAME)
public class CredentialWatchdogExtension implements ServiceExtension {
    public static final String NAME = "VerifiableCredential Watchdog Extension";

    public static final int DEFAULT_WATCHDOG_PERIOD = 60;
    public static final int DEFAULT_WATCHDOG_INITIAL_DELAY = 5;
    public static final int DEFAULT_GRACE_PERIOD_SECONDS = 7 * 24 * 3600; // 1 week
    public static final String CREDENTIAL_WATCHDOG = "CredentialWatchdog";
    private final SecureRandom random = new SecureRandom();

    @Setting(description = "Period (in seconds) at which the Watchdog thread checks all stored credentials for their status. Configuring a number <=0 disables the Watchdog.",
            min = 0, defaultValue = DEFAULT_WATCHDOG_PERIOD + "", key = "edc.iam.credential.status.check.period")
    private int watchdogPeriod;

    @Setting(description = "Initial delay (in seconds) before the Watchdog thread begins its work.",
            min = 0, key = "edc.iam.credential.status.check.delay", required = false)
    private Integer initialDelay;

    @Setting(description = "Grace period (in seconds) before credential expiry at which automatic renewal is triggered", key = "edc.iam.credential.renewal.graceperiod",
            min = 0, defaultValue = DEFAULT_GRACE_PERIOD_SECONDS + "")
    private long gracePeriodSeconds;

    @Inject
    private ExecutorInstrumentation executorInstrumentation;
    @Inject
    private CredentialStatusCheckService credentialStatusCheckService;
    @Inject
    private CredentialStore credentialStore;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private CredentialRequestManager credentialRequestManager;
    private ScheduledExecutorService scheduledExecutorService;
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor().withPrefix(CREDENTIAL_WATCHDOG);

        if (watchdogPeriod > 0) {
            initialDelay = ofNullable(initialDelay).orElseGet((this::randomDelay));
            monitor.debug(() -> "Credential watchdog will run with a delay of %d seconds, at an interval of %d seconds".formatted(initialDelay, watchdogPeriod));
            scheduledExecutorService = executorInstrumentation.instrument(Executors.newSingleThreadScheduledExecutor(), CREDENTIAL_WATCHDOG);
        } else {
            monitor.debug(() -> "The Credential Watchdog is disabled.");
        }
    }

    @Override
    public void start() {
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            monitor.debug(() -> "Starting credential watchdog in %d seconds, every %d seconds".formatted(initialDelay, watchdogPeriod));
            var watchdog = new CredentialWatchdog(credentialStore, credentialStatusCheckService, monitor, transactionContext, Duration.ofSeconds(gracePeriodSeconds), credentialRequestManager);
            scheduledExecutorService.scheduleAtFixedRate(watchdog, initialDelay, watchdogPeriod, TimeUnit.SECONDS);
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
