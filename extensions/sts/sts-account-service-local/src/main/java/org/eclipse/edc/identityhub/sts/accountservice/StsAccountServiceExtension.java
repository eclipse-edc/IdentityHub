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

package org.eclipse.edc.identityhub.sts.accountservice;

import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsAccountService;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsClientSecretGenerator;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.identityhub.sts.accountservice.StsAccountServiceExtension.NAME;


@Extension(value = NAME)
public class StsAccountServiceExtension implements ServiceExtension {
    public static final String NAME = "Local (embedded) STS Account Service Extension";
    @Inject
    private StsAccountStore accountStore;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private Vault vault;
    @Inject(required = false)
    private StsClientSecretGenerator secretGenerator;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public StsAccountService createAccountManager(ServiceExtensionContext context) {
        var monitor = context.getMonitor().withPrefix("STS-Account");
        monitor.info("This IdentityHub runtime contains an embedded SecureTokenService (STS) instance. That means ParticipantContexts and STS Accounts will be synchronized automatically.");
        return new StsAccountServiceImpl(accountStore, transactionContext, vault, ofNullable(secretGenerator).orElseGet(RandomStringGenerator::new));
    }
}
