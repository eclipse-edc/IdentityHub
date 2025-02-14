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

package org.eclipse.edc.issuerservice.issuance.process;

import org.eclipse.edc.issuerservice.spi.issuance.model.IssuerCredentialIssuanceProcess;
import org.eclipse.edc.issuerservice.spi.issuance.process.IssuerCredentialIssuanceProcessManager;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.statemachine.AbstractStateEntityManager;
import org.eclipse.edc.statemachine.StateMachineManager;

public class IssuerCredentialIssuanceProcessManagerImpl extends AbstractStateEntityManager<IssuerCredentialIssuanceProcess, IssuanceProcessStore> implements IssuerCredentialIssuanceProcessManager {


    private IssuerCredentialIssuanceProcessManagerImpl() {
    }

    @Override
    protected StateMachineManager.Builder configureStateMachineManager(StateMachineManager.Builder builder) {
        return builder;
    }

    public static class Builder
            extends AbstractStateEntityManager.Builder<IssuerCredentialIssuanceProcess, IssuanceProcessStore, IssuerCredentialIssuanceProcessManagerImpl, Builder> {

        private Builder() {
            super(new IssuerCredentialIssuanceProcessManagerImpl());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public IssuerCredentialIssuanceProcessManagerImpl build() {
            super.build();
            return manager;
        }
    }
}
