/*
 *  Copyright (c) 2026 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.store.sql.credentialrequest.schema;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class SqlHolderCredentialRequestStoreExtensionTest {

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(TypeManager.class, new JacksonTypeManager());
    }

    @Test
    void createSqlStore_shouldLeaseForConfiguredDuration(ServiceExtensionContext context, ObjectFactory objectFactory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of("edc.iam.credential.request.state-machine.lease-duration-millis", "123000")));
        var extension = objectFactory.constructInstance(SqlHolderCredentialRequestStoreExtension.class);

        var store = extension.createSqlStore(context);

        assertThat(store).extracting("leaseDuration").isEqualTo(Duration.ofMillis(123_000));
    }

    @Test
    void createSqlStore_withoutConfiguration_shouldLeaseForDefaultDuration(ServiceExtensionContext context, ObjectFactory objectFactory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of()));
        var extension = objectFactory.constructInstance(SqlHolderCredentialRequestStoreExtension.class);

        var store = extension.createSqlStore(context);

        assertThat(store).extracting("leaseDuration").isEqualTo(Duration.ofMinutes(5));
    }
}
