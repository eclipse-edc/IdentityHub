/*
 *  Copyright (c) 2026 Metaform Systems Inc.
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

package org.eclipse.edc.issuerservice.credentials;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerProfile;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpProfile;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC2_0_JOSE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class CredentialDefaultServiceExtensionTest {

    private final ExecutorInstrumentation executorInstrumentation = mock();
    private final ScheduledExecutorService renewalExecutor = mock();
    private final CredentialStore store = mock();
    private final Monitor monitor = mock();
    private DcpProfile issuerProfile = new DcpProfile("vc20-bssl/jwt", VC2_0_JOSE, "BitstringStatusListEntry");

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(ExecutorInstrumentation.class, executorInstrumentation);
        context.registerService(CredentialStore.class, store);
        context.registerService(Clock.class, Clock.systemUTC());
        context.registerService(DcpIssuerProfile.class, () -> issuerProfile);
        context.registerService(Monitor.class, monitor);
        when(executorInstrumentation.instrument(any(ScheduledExecutorService.class), anyString())).thenReturn(renewalExecutor);
        when(store.query(any())).thenReturn(StoreResult.success(List.of()));
    }

    @Test
    void start_shouldRenewExpiringStatusListsPeriodically(ServiceExtensionContext context, ObjectFactory objectFactory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of("edc.issuer.statuslist.renewal.period", "3600")));
        var extension = objectFactory.constructInstance(CredentialDefaultServiceExtension.class);
        extension.statusListManager();

        extension.start();

        var task = ArgumentCaptor.forClass(Runnable.class);
        verify(renewalExecutor).scheduleAtFixedRate(task.capture(), eq(0L), eq(3600L), eq(TimeUnit.SECONDS));
        task.getValue().run();
        verify(store, atLeastOnce()).query(any());

        extension.shutdown();
        verify(renewalExecutor).shutdownNow();
    }

    @Test
    void start_whenRenewingFails_shouldKeepRenewingPeriodically(ServiceExtensionContext context, ObjectFactory objectFactory) {
        when(store.query(any())).thenThrow(new EdcPersistenceException("database unavailable"));
        var extension = objectFactory.constructInstance(CredentialDefaultServiceExtension.class);
        extension.statusListManager();

        extension.start();

        var task = ArgumentCaptor.forClass(Runnable.class);
        verify(renewalExecutor).scheduleAtFixedRate(task.capture(), eq(0L), eq(24 * 60 * 60L), eq(TimeUnit.SECONDS));
        // an exception would end the periodic execution
        assertThatNoException().isThrownBy(() -> task.getValue().run());
    }

    @Test
    void start_whenRenewalDeactivated_shouldNotRenew(ServiceExtensionContext context, ObjectFactory objectFactory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of("edc.issuer.statuslist.renewal.period", "0")));
        var extension = objectFactory.constructInstance(CredentialDefaultServiceExtension.class);
        extension.statusListManager();

        extension.start();

        verifyNoInteractions(executorInstrumentation);
        // issuing a credential does not renew the status list it is added to, unless it has expired
        verify(monitor).warning(contains("only renewed when a credential on it is revoked, or when a new credential is added to it"));
    }

    @Test
    void statusListManager_shouldValidateRenewalSettings(ServiceExtensionContext context, ObjectFactory objectFactory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                "edc.issuer.statuslist.renewal.period", "7200",
                "edc.issuer.statuslist.renewal.threshold", "0")));
        var extension = objectFactory.constructInstance(CredentialDefaultServiceExtension.class);

        extension.statusListManager();
        extension.start();

        verify(monitor).warning(contains("renewal threshold of 0 seconds is not positive"));
        verify(renewalExecutor).scheduleAtFixedRate(any(), eq(0L), eq(7200L), eq(TimeUnit.SECONDS));
    }

    @Test
    void start_whenDefaultStatusListManagerNotUsed_shouldNotRenew(ObjectFactory objectFactory) {
        var extension = objectFactory.constructInstance(CredentialDefaultServiceExtension.class);

        // another StatusListManager takes care of its own status list credentials
        extension.start();

        verifyNoInteractions(executorInstrumentation);
    }

    @Test
    void statusListManager_whenStatusListTypeOfProfileNotSupported_shouldFail(ObjectFactory objectFactory) {
        issuerProfile = new DcpProfile("custom/jwt", VC2_0_JOSE, "CustomStatusListEntry");
        var extension = objectFactory.constructInstance(CredentialDefaultServiceExtension.class);

        // the runtime does not start with a profile that needs another StatusListManager
        assertThatThrownBy(extension::statusListManager).isInstanceOf(EdcException.class)
                .hasMessageContaining("CustomStatusListEntry").hasMessageContaining("custom/jwt");
    }
}
