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

package org.eclipse.edc.issuerservice.issuance;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.issuerservice.issuance.process.IssuanceProcessManagerImpl;
import org.eclipse.edc.issuerservice.issuance.process.IssuanceProcessServiceImpl;
import org.eclipse.edc.issuerservice.spi.credentials.CredentialStatusService;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.store.CredentialDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.delivery.CredentialStorageClient;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.process.IssuanceProcessManager;
import org.eclipse.edc.issuerservice.spi.issuance.process.IssuanceProcessService;
import org.eclipse.edc.issuerservice.spi.issuance.process.retry.IssuanceProcessRetryStrategy;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.SettingContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.statemachine.StateMachineConfiguration;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;

import static org.eclipse.edc.issuerservice.issuance.IssuanceCoreExtension.NAME;

@Extension(NAME)
public class IssuanceCoreExtension implements ServiceExtension {

    public static final String NAME = "Issuance Core Extension";

    @SettingContext("edc.issuer.issuance")
    @Configuration
    private StateMachineConfiguration stateMachineConfiguration;

    private IssuanceProcessManager issuanceProcessManager;

    @Inject
    private IssuanceProcessStore issuanceProcessStore;

    @Inject
    private CredentialGeneratorRegistry credentialGenerator;

    @Inject
    private CredentialDefinitionStore credentialDefinitionStore;

    @Inject
    private CredentialStore credentialStore;

    @Inject
    private Monitor monitor;

    @Inject
    private Telemetry telemetry;

    @Inject
    private ExecutorInstrumentation executorInstrumentation;

    @Inject(required = false)
    private IssuanceProcessRetryStrategy retryStrategy;

    @Inject
    private CredentialStorageClient credentialStorageClient;

    @Inject
    private Clock clock;

    @Inject
    private TransactionContext transactionContext;
    @Inject
    private CredentialStatusService credentialStatusService;

    @Provider
    public IssuanceProcessManager createIssuanceProcessManager() {

        if (issuanceProcessManager == null) {
            var waitStrategy = retryStrategy != null ? retryStrategy : stateMachineConfiguration.iterationWaitExponentialWaitStrategy();
            issuanceProcessManager = IssuanceProcessManagerImpl.Builder.newInstance()
                    .store(issuanceProcessStore)
                    .monitor(monitor)
                    .batchSize(stateMachineConfiguration.batchSize())
                    .waitStrategy(waitStrategy)
                    .telemetry(telemetry)
                    .clock(clock)
                    .executorInstrumentation(executorInstrumentation)
                    .credentialGeneratorRegistry(credentialGenerator)
                    .credentialDefinitionStore(credentialDefinitionStore)
                    .credentialStore(credentialStore)
                    .credentialStorageClient(credentialStorageClient)
                    .credentialStatusService(credentialStatusService)
                    .entityRetryProcessConfiguration(stateMachineConfiguration.entityRetryProcessConfiguration())
                    .build();
        }
        return issuanceProcessManager;
    }

    @Provider
    public IssuanceProcessService createIssuanceProcessService() {
        return new IssuanceProcessServiceImpl(transactionContext, issuanceProcessStore);
    }

    @Override
    public void start() {
        issuanceProcessManager.start();
    }

    @Override
    public void shutdown() {
        if (issuanceProcessManager != null) {
            issuanceProcessManager.stop();
        }
    }
}
