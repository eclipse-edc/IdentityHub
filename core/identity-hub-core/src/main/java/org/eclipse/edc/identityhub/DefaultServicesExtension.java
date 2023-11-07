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

import org.eclipse.edc.identityhub.defaults.InMemoryCredentialStore;
import org.eclipse.edc.identityhub.spi.generator.PresentationGenerator;
import org.eclipse.edc.identityhub.spi.resolution.CredentialQueryResolver;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

@Extension("Default Services Extension")
public class DefaultServicesExtension implements ServiceExtension {
    @Provider(isDefault = true)
    public CredentialStore createInMemStore() {
        return new InMemoryCredentialStore();

    }

    @Provider(isDefault = true)
    public CredentialQueryResolver createCredentialResolver(ServiceExtensionContext context) {
        context.getMonitor().warning("  #### Creating a default NOOP CredentialQueryResolver, that will always return 'null'!");
        return (query, issuerScopes) -> null;
    }

    @Provider(isDefault = true)
    public PresentationGenerator createPresentationGenerator(ServiceExtensionContext context) {
        context.getMonitor().warning("  #### Creating a default NOOP PresentationGenerator, that will always return 'null'!");
        return (credentials, presentationDefinition) -> null;
    }
}
