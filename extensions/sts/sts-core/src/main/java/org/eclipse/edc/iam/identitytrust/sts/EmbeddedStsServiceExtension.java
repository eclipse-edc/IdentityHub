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

package org.eclipse.edc.iam.identitytrust.sts;

import org.eclipse.edc.iam.identitytrust.sts.service.EmbeddedSecureTokenService;
import org.eclipse.edc.iam.identitytrust.sts.service.StsClientTokenGeneratorServiceImpl;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsAccountService;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsClientTokenGeneratorService;
import org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService;
import org.eclipse.edc.jwt.signer.spi.JwsSignerProvider;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

import static org.eclipse.edc.identityhub.sts.accountservice.StsAccountServiceExtension.NAME;


@Extension(value = NAME)
public class EmbeddedStsServiceExtension implements ServiceExtension {
    public static final String NAME = "Local (embedded) STS Account Service Extension";
    private static final int DEFAULT_STS_TOKEN_EXPIRATION_MIN = 5;
    @Inject
    private Clock clock;
    @Inject
    private JwsSignerProvider externalSigner;
    @Setting(description = "Self-issued ID Token expiration in minutes. By default is 5 minutes", defaultValue = "" + DEFAULT_STS_TOKEN_EXPIRATION_MIN, key = "edc.iam.sts.token.expiration")
    private long stsTokenExpirationMin;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private StsAccountService stsAccountService;
    private EmbeddedSecureTokenService embeddedSts;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public ParticipantSecureTokenService secureTokenService() {
        if (embeddedSts == null) {
            embeddedSts = new EmbeddedSecureTokenService(transactionContext, TimeUnit.MINUTES.toSeconds(stsTokenExpirationMin), new JwtGenerationService(externalSigner), clock, stsAccountService);
        }
        return embeddedSts;
    }

    @Provider
    public StsClientTokenGeneratorService clientTokenService(ServiceExtensionContext context) {
        return new StsClientTokenGeneratorServiceImpl(
                TimeUnit.MINUTES.toSeconds(stsTokenExpirationMin),
                secureTokenService());
    }
}
