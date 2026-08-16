/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.iam.decentralizedclaims.sts;

import org.eclipse.edc.iam.decentralizedclaims.sts.service.StsClientTokenGeneratorServiceImpl;
import org.eclipse.edc.iam.decentralizedclaims.sts.spi.service.StsClientTokenGeneratorService;
import org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;

import java.util.concurrent.TimeUnit;

@Extension(value = StsClientTokenGeneratorExtension.NAME)
public class StsClientTokenGeneratorExtension implements ServiceExtension {
    public static final String NAME = "STS Client Token Generator Extension";
    private static final int DEFAULT_STS_TOKEN_EXPIRATION_MIN = 5;
    @Setting(description = "Self-issued ID Token expiration in minutes. By default is 5 minutes", defaultValue = "" + DEFAULT_STS_TOKEN_EXPIRATION_MIN, key = "edc.iam.sts.token.expiration")
    private long stsTokenExpirationMin;
    @Inject
    private ParticipantSecureTokenService secureTokenService;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public StsClientTokenGeneratorService clientTokenService() {
        return new StsClientTokenGeneratorServiceImpl(
                TimeUnit.MINUTES.toSeconds(stsTokenExpirationMin),
                secureTokenService);
    }
}
