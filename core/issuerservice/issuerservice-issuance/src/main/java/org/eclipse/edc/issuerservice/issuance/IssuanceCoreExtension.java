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

import org.eclipse.edc.issuerservice.issuance.process.IssuerCredentialIssuanceProcessServiceImpl;
import org.eclipse.edc.issuerservice.issuance.process.IssuerCredentialIssuanceProcessManagerImpl;
import org.eclipse.edc.issuerservice.spi.issuance.process.IssuerCredentialIssuanceProcessService;
import org.eclipse.edc.issuerservice.spi.issuance.process.IssuerCredentialIssuanceProcessManager;
import org.eclipse.edc.issuerservice.spi.issuance.process.retry.IssuanceProcessRetryStrategy;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;

import static org.eclipse.edc.issuerservice.issuance.IssuanceCoreExtension.NAME;
import static org.eclipse.edc.statemachine.AbstractStateEntityManager.DEFAULT_BATCH_SIZE;
import static org.eclipse.edc.statemachine.AbstractStateEntityManager.DEFAULT_ITERATION_WAIT;
import static org.eclipse.edc.statemachine.AbstractStateEntityManager.DEFAULT_SEND_RETRY_BASE_DELAY;
import static org.eclipse.edc.statemachine.AbstractStateEntityManager.DEFAULT_SEND_RETRY_LIMIT;

@Extension(NAME)
public class IssuanceCoreExtension implements ServiceExtension {

    public static final String NAME = "Issuance Core Extension";


    @Setting(description = "The iteration wait time in milliseconds in the issuance process state machine. Default value " + DEFAULT_ITERATION_WAIT,
            key = "edc.issuer.issuance.state-machine.iteration-wait-millis",
            defaultValue = DEFAULT_ITERATION_WAIT + "")
    private long stateMachineIterationWaitMillis;

    @Setting(description = "The batch size in the issuance process state machine. Default value " + DEFAULT_BATCH_SIZE, key = "edc.issuer.issuance.state-machine.batch-size", defaultValue = DEFAULT_BATCH_SIZE + "")
    private int stateMachineBatchSize;

    @Setting(description = "How many times a specific operation must be tried before terminating the issuance with error", key = "edc.issuer.issuance.send.retry.limit", defaultValue = DEFAULT_SEND_RETRY_LIMIT + "")
    private int sendRetryLimit;

    @Setting(description = "The base delay for the issuance retry mechanism in millisecond", key = "edc.issuer.issuance.send.retry.base-delay.ms", defaultValue = DEFAULT_SEND_RETRY_BASE_DELAY + "")
    private long sendRetryBaseDelay;

    private IssuerCredentialIssuanceProcessManager issuerCredentialIssuanceProcessManager;

    @Inject
    private IssuanceProcessStore issuanceProcessStore;

    @Inject
    private Monitor monitor;

    @Inject
    private Telemetry telemetry;

    @Inject
    private ExecutorInstrumentation executorInstrumentation;

    @Inject(required = false)
    private IssuanceProcessRetryStrategy retryStrategy;

    @Inject
    private Clock clock;

    @Inject
    private TransactionContext transactionContext;

    @Provider
    public IssuerCredentialIssuanceProcessManager createIssuanceProcessManager() {

        if (issuerCredentialIssuanceProcessManager == null) {
            var waitStrategy = retryStrategy != null ? retryStrategy : new ExponentialWaitStrategy(stateMachineIterationWaitMillis);
            issuerCredentialIssuanceProcessManager = IssuerCredentialIssuanceProcessManagerImpl.Builder.newInstance()
                    .store(issuanceProcessStore)
                    .monitor(monitor)
                    .batchSize(stateMachineBatchSize)
                    .waitStrategy(waitStrategy)
                    .telemetry(telemetry)
                    .clock(clock)
                    .executorInstrumentation(executorInstrumentation)
                    .entityRetryProcessConfiguration(getEntityRetryProcessConfiguration())
                    .build();
        }
        return issuerCredentialIssuanceProcessManager;
    }

    @Provider
    public IssuerCredentialIssuanceProcessService createIssuanceProcessService() {
        return new IssuerCredentialIssuanceProcessServiceImpl(transactionContext, issuanceProcessStore);
    }

    @Override
    public void start() {
        issuerCredentialIssuanceProcessManager.start();
    }

    @Override
    public void shutdown() {
        if (issuerCredentialIssuanceProcessManager != null) {
            issuerCredentialIssuanceProcessManager.stop();
        }
    }


    @NotNull
    private EntityRetryProcessConfiguration getEntityRetryProcessConfiguration() {
        return new EntityRetryProcessConfiguration(sendRetryLimit, () -> new ExponentialWaitStrategy(sendRetryBaseDelay));
    }
}
