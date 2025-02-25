/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.identitytrust.sts.defaults;

import org.eclipse.edc.iam.identitytrust.sts.defaults.service.StsClientTokenGeneratorServiceImpl;
import org.eclipse.edc.iam.identitytrust.sts.defaults.store.InMemoryStsAccountStore;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsClientTokenGeneratorService;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.identityhub.spi.authentication.ParticipantSecureTokenService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.concurrent.TimeUnit;

@Extension(StsDefaultServicesExtension.NAME)
public class StsDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Secure Token Service Default Services";
    private static final int DEFAULT_STS_TOKEN_EXPIRATION_MIN = 5;
    @Setting(description = "Self-issued ID Token expiration in minutes. By default is 5 minutes", defaultValue = "" + StsDefaultServicesExtension.DEFAULT_STS_TOKEN_EXPIRATION_MIN, key = "edc.iam.sts.token.expiration")
    private int tokenExpirationMinutes;
    @Inject
    private CriterionOperatorRegistry criterionOperatorRegistry;
    @Inject
    private ParticipantSecureTokenService secureTokenService;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public StsClientTokenGeneratorService clientTokenService(ServiceExtensionContext context) {
        return new StsClientTokenGeneratorServiceImpl(
                TimeUnit.MINUTES.toSeconds(tokenExpirationMinutes),
                secureTokenService);
    }

    @Provider(isDefault = true)
    public StsAccountStore clientStore() {
        return new InMemoryStsAccountStore(criterionOperatorRegistry);
    }


}
