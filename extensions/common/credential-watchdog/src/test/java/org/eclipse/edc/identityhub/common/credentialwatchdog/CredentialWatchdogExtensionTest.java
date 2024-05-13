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


import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.eclipse.edc.identityhub.common.credentialwatchdog.CredentialWatchdogExtension.CREDENTIAL_WATCHDOG;
import static org.eclipse.edc.identityhub.common.credentialwatchdog.CredentialWatchdogExtension.WATCHDOG_DELAY_PROPERTY;
import static org.eclipse.edc.identityhub.common.credentialwatchdog.CredentialWatchdogExtension.WATCHDOG_PERIOD_PROPERTY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class CredentialWatchdogExtensionTest {


    private final ExecutorInstrumentation executorInstrumentationMock = mock();
    private Monitor monitor;

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        monitor = mock(Monitor.class);
        when(monitor.withPrefix(any())).thenReturn(monitor);
        context.registerService(Monitor.class, monitor);
        context.registerService(ExecutorInstrumentation.class, executorInstrumentationMock);
    }

    @DisplayName("Disable watchdog on negative or zero second period")
    @ParameterizedTest(name = "Disable on delay of {0} seconds")
    @ValueSource(ints = { 0, -1, -100 })
    void initialize_whenNegativePeriod_shouldDisable(int period, CredentialWatchdogExtension extension, ServiceExtensionContext context) {
        when(context.getSetting(eq(WATCHDOG_PERIOD_PROPERTY), anyInt())).thenReturn(period);
        extension.initialize(context);

        verifyNoInteractions(executorInstrumentationMock);
        verify(monitor).debug(ArgumentMatchers.<Supplier<String>>argThat(stringSupplier -> stringSupplier.get().contains("Credential Watchdog is disabled")));
    }

    @DisplayName("Verify random delay [1..5] if no initial delay is configured")
    @Test
    void initialize_whenNoDelay_shouldUseRandomBetweenZeroAndFive(CredentialWatchdogExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(monitor).debug(ArgumentMatchers.<Supplier<String>>argThat(stringSupplier ->
                stringSupplier.get().matches(".* delay of ([1-5]) seconds, at an interval of 60 seconds")));
        verify(executorInstrumentationMock).instrument(any(), eq(CREDENTIAL_WATCHDOG));
    }

    @DisplayName("Verify a configured delay is used")
    @Test
    void initialize_whenDelay_shouldUseConfiguredDelay(CredentialWatchdogExtension extension, ServiceExtensionContext context) {
        when(context.getSetting(eq(WATCHDOG_DELAY_PROPERTY), anyInt())).thenReturn(42);

        extension.initialize(context);
        verify(monitor).debug(ArgumentMatchers.<Supplier<String>>argThat(stringSupplier ->
                stringSupplier.get().endsWith("delay of 42 seconds, at an interval of 60 seconds")));
        verify(executorInstrumentationMock).instrument(any(), eq(CREDENTIAL_WATCHDOG));
    }

    @DisplayName("Verify the watchdog is not start if a <=0 period is configured")
    @Test
    void start_whenWatchdogDisabled_shouldNotStart(CredentialWatchdogExtension extension, ServiceExtensionContext context) {
        when(context.getSetting(eq(WATCHDOG_PERIOD_PROPERTY), anyInt())).thenReturn(-10);

        extension.initialize(context);
        extension.start();
        verifyNoInteractions(executorInstrumentationMock);
        verify(monitor, never()).debug(ArgumentMatchers.<Supplier<String>>argThat(stringSupplier ->
                stringSupplier.get().startsWith("Starting credential watchdog")));
    }

    @DisplayName("Verify watchdog starts when properly configured, at the expected timing intervals")
    @Test
    void start_shouldStartWatchdog(CredentialWatchdogExtension extension, ServiceExtensionContext context) {
        when(context.getSetting(eq(WATCHDOG_PERIOD_PROPERTY), anyInt())).thenReturn(1);
        when(context.getSetting(eq(WATCHDOG_DELAY_PROPERTY), anyInt())).thenReturn(1);
        var executorMock = mock(ScheduledExecutorService.class);
        when(executorInstrumentationMock.instrument(any(), eq(CREDENTIAL_WATCHDOG)))
                .thenReturn(executorMock);
        when(executorMock.isShutdown()).thenReturn(false);
        extension.initialize(context);
        extension.start();

        verify(executorMock).scheduleAtFixedRate(isA(CredentialWatchdog.class), eq(1L), eq(1L), eq(TimeUnit.SECONDS));
        verify(monitor).debug(ArgumentMatchers.<Supplier<String>>argThat(stringSupplier ->
                stringSupplier.get().startsWith("Starting credential watchdog")));
    }

    @DisplayName("Verify shutting down the extension is a NOOP if the watchdog is not started")
    @Test
    void shutdown_whenNotRunning_shouldNoop(CredentialWatchdogExtension extension, ServiceExtensionContext context) {
        when(context.getSetting(eq(WATCHDOG_PERIOD_PROPERTY), anyInt())).thenReturn(-1); // executor will not be initialized
        when(context.getSetting(eq(WATCHDOG_DELAY_PROPERTY), anyInt())).thenReturn(1);
        var executorMock = mock(ScheduledExecutorService.class);

        extension.initialize(context);

        extension.shutdown();

        verify(executorInstrumentationMock, never()).instrument(any(), eq(CREDENTIAL_WATCHDOG));
    }

    @DisplayName("Verify shutting down the extension stops the watchdog thread")
    @Test
    void shutdown_whenRunning_shouldStop(CredentialWatchdogExtension extension, ServiceExtensionContext context) {
        when(context.getSetting(eq(WATCHDOG_PERIOD_PROPERTY), anyInt())).thenReturn(1);
        when(context.getSetting(eq(WATCHDOG_DELAY_PROPERTY), anyInt())).thenReturn(1);
        var executorMock = mock(ScheduledExecutorService.class);

        when(executorInstrumentationMock.instrument(any(), eq(CREDENTIAL_WATCHDOG))).thenReturn(executorMock);
        when(executorMock.isShutdown()).thenReturn(false);
        extension.initialize(context);

        extension.start();
        extension.shutdown();

        verify(executorMock).scheduleAtFixedRate(isA(CredentialWatchdog.class), eq(1L), eq(1L), eq(TimeUnit.SECONDS));
        verify(executorMock).shutdownNow();
    }
}