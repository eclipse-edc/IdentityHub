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

package org.eclipse.edc.identityhub;

import org.eclipse.edc.identityhub.defaults.DiscriminatorMappingRegistryImpl;
import org.eclipse.edc.identityhub.spi.transformation.DiscriminatorMappingRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.identityhub.DefaultServicesExtension.NAME;

@Extension(NAME)
public class DiscriminatorMappingExtension implements ServiceExtension {

    public static final String CONFIG_PREFIX = "edc.identityhub.discriminator";
    public static final String DISCRIMINATOR_ALIAS = CONFIG_PREFIX + ".<alias>.";

    @Setting(context = DISCRIMINATOR_ALIAS, description = "the full value for the discriminator")
    public static final String DISCRIMINATOR_ALIAS_VALUE_SUFFIX = "value";

    @Setting(context = DISCRIMINATOR_ALIAS, description = "the discriminator alias")
    public static final String DISCRIMINATOR_ALIAS_SUFFIX = "alias";

    public static final String NAME = "Discriminator Mapping Extension";

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public DiscriminatorMappingRegistry createDiscriminatorMappingRegistry(ServiceExtensionContext context) {
        var config = context.getConfig(CONFIG_PREFIX);

        var configs = config.partition().toList();

        var discriminatorMappingRegistry = new DiscriminatorMappingRegistryImpl();
        configs.forEach(c -> {
            var value = c.getString(DISCRIMINATOR_ALIAS_VALUE_SUFFIX);
            var alias = c.getString(DISCRIMINATOR_ALIAS_SUFFIX);
            discriminatorMappingRegistry.addMapping(alias, value);
        });

        return discriminatorMappingRegistry;
    }
}
