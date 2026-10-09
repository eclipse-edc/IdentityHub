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

package org.eclipse.edc.identityhub;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.identityhub.accesstoken.rules.ClaimIsPresentRule;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.rules.JtiValidationRule;
import org.eclipse.edc.token.rules.NotBeforeValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.eclipse.edc.identityhub.DefaultServicesExtension.ACCESSTOKEN_JTI_VALIDATION_ACTIVATE;
import static org.eclipse.edc.identityhub.DefaultServicesExtension.JTI_CLEANUP_PERIOD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DefaultServicesExtensionTest {
    private final TokenValidationRulesRegistry registry = mock();
    private final JtiValidationStore jtiValidationStore = mock();
    private final ExecutorInstrumentation executorInstrumentation = mock();
    private final ScheduledExecutorService jtiEntryReaper = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(TokenValidationRulesRegistry.class, registry);
        context.registerService(JtiValidationStore.class, jtiValidationStore);
        context.registerService(ExecutorInstrumentation.class, executorInstrumentation);
        when(executorInstrumentation.instrument(any(ScheduledExecutorService.class), anyString())).thenReturn(jtiEntryReaper);
    }

    @Test
    void initialize_verifyTokenRules(DefaultServicesExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);
        verify(registry).addRule(eq("dcp-si"), isA(ClaimIsPresentRule.class));
        verify(registry).addRule(eq("dcp-si"), isA(ExpirationIssuedAtValidationRule.class));
        verify(registry).addRule(eq("dcp-si"), isA(NotBeforeValidationRule.class));
        verify(registry).addRule(eq("dcp-access-token"), isA(ClaimIsPresentRule.class));
        verifyNoMoreInteractions(registry);
    }

    @Test
    void initialize_verifyTokenRules_withJtiRule(ServiceExtensionContext context, ObjectFactory factory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(ACCESSTOKEN_JTI_VALIDATION_ACTIVATE, Boolean.TRUE.toString())));


        factory.constructInstance(DefaultServicesExtension.class).initialize(context);
        verify(registry).addRule(eq("dcp-si"), isA(ClaimIsPresentRule.class));
        verify(registry).addRule(eq("dcp-si"), isA(ExpirationIssuedAtValidationRule.class));
        verify(registry).addRule(eq("dcp-si"), isA(NotBeforeValidationRule.class));
        verify(registry).addRule(eq("dcp-si"), isA(JtiValidationRule.class));
        verify(registry).addRule(eq("dcp-access-token"), isA(ClaimIsPresentRule.class));
        verify(registry).addRule(eq("dcp-access-token"), isA(JtiValidationRule.class));
        verifyNoMoreInteractions(registry);
    }

    @Test
    void start_whenJtiCheckActivated_shouldDeleteExpiredEntriesPeriodically(ServiceExtensionContext context, ObjectFactory factory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(ACCESSTOKEN_JTI_VALIDATION_ACTIVATE, "true", JTI_CLEANUP_PERIOD, "30")));
        when(jtiValidationStore.deleteExpired()).thenReturn(StoreResult.success(1));
        var extension = factory.constructInstance(DefaultServicesExtension.class);
        extension.initialize(context);

        extension.start();

        var task = ArgumentCaptor.forClass(Runnable.class);
        verify(jtiEntryReaper).scheduleAtFixedRate(task.capture(), eq(30L), eq(30L), eq(TimeUnit.SECONDS));
        task.getValue().run();
        verify(jtiValidationStore).deleteExpired();

        extension.shutdown();
        verify(jtiEntryReaper).shutdownNow();
    }

    @Test
    void start_whenDeletingExpiredEntriesThrows_shouldKeepDeletingPeriodically(ServiceExtensionContext context, ObjectFactory factory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(ACCESSTOKEN_JTI_VALIDATION_ACTIVATE, "true")));
        when(jtiValidationStore.deleteExpired()).thenThrow(new EdcPersistenceException("foo"));
        var extension = factory.constructInstance(DefaultServicesExtension.class);
        extension.initialize(context);

        extension.start();

        var task = ArgumentCaptor.forClass(Runnable.class);
        verify(jtiEntryReaper).scheduleAtFixedRate(task.capture(), eq(60L), eq(60L), eq(TimeUnit.SECONDS));
        // an exception would end the periodic execution
        assertThatNoException().isThrownBy(() -> task.getValue().run());
    }

    @Test
    void start_whenJtiCheckNotActivated_shouldNotDeleteExpiredEntries(DefaultServicesExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        extension.start();

        verifyNoInteractions(executorInstrumentation, jtiValidationStore);
    }
}
