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

package org.eclipse.edc.identityhub.core;

import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialValidityCheckService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.eclipse.edc.identityhub.core.CredentialWatchdogExtension.NAME;

@Extension(value = NAME)
public class CredentialWatchdogExtension implements ServiceExtension {
    public static final String NAME = "VerifiableCredential Watchdog Extension";

    public static final int DEFAULT_WATCHDOG_PERIOD = 60;
    @Setting(value = "Period (in seconds) at which the Watchdog thread checks all stored credentials for their status. Configuring a number <=0 disables the Watchdog.",
            type = "integer", min = 0, defaultValue = DEFAULT_WATCHDOG_PERIOD + "")
    public static final String WATCHDOG_PERIOD_PROPERTY = "edc.iam.credential.status.check.period";
    public static final String CREDENTIAL_WATCHDOG = "CredentialWatchdog";

    @Inject
    private ExecutorInstrumentation executorInstrumentation;
    @Inject
    private CredentialValidityCheckService credentialValidityCheckService;
    @Inject
    private CredentialStore credentialStore;

    private ScheduledExecutorService scheduledExecutorService;
    private Integer watchdogPeriod;
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        watchdogPeriod = context.getConfig().getInteger(WATCHDOG_PERIOD_PROPERTY, DEFAULT_WATCHDOG_PERIOD);
        monitor = context.getMonitor().withPrefix(CREDENTIAL_WATCHDOG);

        if (watchdogPeriod <= 0) {
            monitor.debug("Config property '%s' was <= 0 (%d). The Credential Watchdog is disabled.".formatted(WATCHDOG_PERIOD_PROPERTY, watchdogPeriod));
        } else {
            scheduledExecutorService = executorInstrumentation.instrument(Executors.newSingleThreadScheduledExecutor(), CREDENTIAL_WATCHDOG);
        }
    }

    @Override
    public void start() {
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                //todo: add transaction here
                var allCredentials = credentialStore.query(QuerySpec.max())
                        .onFailure(f -> monitor.warning("Failed to fetch credentials from database: %s".formatted(f.getFailureDetail())))
                        .orElse(f -> Collections.emptyList());

                allCredentials.forEach(credential -> {
                    var status = credentialValidityCheckService.checkStatus(credential)
                            .orElse(f -> {
                                monitor.warning("Error determining status for credential '%s': %s. Will move to the ERROR state.".formatted(credential.getId(), f.getFailureDetail()));
                                return VcStatus.ERROR;
                            });
                    credential.setCredentialStatus(status);
                    credentialStore.update(credential);
                });

            }, 0, watchdogPeriod, TimeUnit.SECONDS);
        }
    }
}
