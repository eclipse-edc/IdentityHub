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

import org.eclipse.edc.iam.identitytrust.sts.defaults.service.StsAccountServiceImpl;
import org.eclipse.edc.iam.identitytrust.sts.defaults.service.StsClientTokenGeneratorServiceImpl;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DependencyInjectionExtension.class)
public class StsDefaultServicesExtensionTest {

    @Test
    void initialize(StsDefaultServicesExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        assertThat(extension.clientService()).isInstanceOf(StsAccountServiceImpl.class);
        assertThat(extension.clientTokenService(context)).isInstanceOf(StsClientTokenGeneratorServiceImpl.class);
    }
}
