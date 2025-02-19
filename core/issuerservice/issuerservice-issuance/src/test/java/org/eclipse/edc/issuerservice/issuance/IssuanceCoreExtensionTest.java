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


import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.issuerservice.issuance.process.IssuanceProcessManagerImpl;
import org.eclipse.edc.issuerservice.issuance.process.IssuanceProcessServiceImpl;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class IssuanceCoreExtensionTest {


    @Test
    void verifyProviders(ServiceExtensionContext context, ObjectFactory factory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of("edc.issuer.id", "did::web:issuer")));

        var extension = factory.constructInstance(IssuanceCoreExtension.class);
        assertThat(extension.createIssuanceProcessManager()).isInstanceOf(IssuanceProcessManagerImpl.class);
        assertThat(extension.createIssuanceProcessService()).isInstanceOf(IssuanceProcessServiceImpl.class);

    }
}
