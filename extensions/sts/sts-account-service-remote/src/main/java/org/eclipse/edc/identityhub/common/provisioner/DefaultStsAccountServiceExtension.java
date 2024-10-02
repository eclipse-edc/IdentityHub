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

package org.eclipse.edc.identityhub.common.provisioner;

import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.identityhub.spi.participantcontext.StsAccountService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static org.eclipse.edc.identityhub.common.provisioner.DefaultStsAccountServiceExtension.NAME;

@Extension(value = NAME)
public class DefaultStsAccountServiceExtension implements ServiceExtension {
    public static final String NAME = "Default STS Account Service Extension";
    @Inject(required = false)
    private StsAccountStore accountStore;
    @Inject
    private TransactionContext transactionContext;

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public StsAccountService createAccountManager(ServiceExtensionContext context) {
        var monitor = context.getMonitor().withPrefix("STS-Account");

        if (accountStore != null) {
            monitor.info("This IdentityHub runtime contains an embedded SecureTokenService (STS) instance. That means ParticipantContexts and STS Accounts will be synchronized automatically.");
            return new LocalStsAccountService(accountStore, transactionContext);
        } else {
            monitor.warning("This IdentityHub runtime does NOT contain an embedded SecureTokenService (STS) instance and no remote STS was configured. " +
                    "Synchronizing ParticipantContexts and STS Accounts must be handled out-of-band.");
            return new NoopAccountService();
        }
    }

    private static class NoopAccountService implements StsAccountService {
        @Override
        public ServiceResult<Void> createAccount(ParticipantManifest manifest, String secretAlias) {
            return ServiceResult.success();
        }

        @Override
        public ServiceResult<Void> deleteAccount(String participantId) {
            return ServiceResult.success();
        }

        @Override
        public ServiceResult<Void> updateAccount(StsAccount modificationFunction) {
            return ServiceResult.success();
        }

        @Override
        public ServiceResult<StsAccount> findById(String id) {
            return ServiceResult.success(null);
        }
    }
}
