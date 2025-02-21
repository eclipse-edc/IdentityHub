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

package org.eclipse.edc.identityhub.sts;

import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService;
import org.eclipse.edc.identityhub.spi.participantcontext.StsAccountService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static org.eclipse.edc.identityhub.sts.RemoteStsServiceExtension.NAME;

@Extension(value = NAME)
public class RemoteStsServiceExtension implements ServiceExtension {
    public static final String NAME = "Remote Secure Token Service extension";

    @Setting(key = "edc.iam.sts.oauth.token.url", description = "STS OAuth2 endpoint for requesting a token")
    private String tokenUrl;

    @Inject
    private Oauth2Client oauth2Client;

    @Inject
    private TransactionContext transactionContext;
    @Inject
    private Vault vault;
    @Inject
    private StsAccountService stsAccountService;

    @Provider
    public ParticipantSecureTokenService createRemoteSecureTokenService() {
        return new RemoteSecureTokenService(oauth2Client, transactionContext, vault, tokenUrl, stsAccountService);
    }
}
