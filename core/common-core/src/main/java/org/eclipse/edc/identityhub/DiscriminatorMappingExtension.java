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

package org.eclipse.edc.identityhub;

import org.eclipse.edc.identityhub.defaults.DiscriminatorMappingRegistryImpl;
import org.eclipse.edc.identityhub.spi.transformation.DiscriminatorMappingRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;

import static org.eclipse.edc.identityhub.DefaultServicesExtension.NAME;

@Extension(NAME)
public class DiscriminatorMappingExtension implements ServiceExtension {

    public static final String NAME = "Discriminator Mapping Extension";

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public DiscriminatorMappingRegistry createDiscriminatorMappingRegistry() {
        //todo: read config and pre-initialize
        return new DiscriminatorMappingRegistryImpl();
    }
}
