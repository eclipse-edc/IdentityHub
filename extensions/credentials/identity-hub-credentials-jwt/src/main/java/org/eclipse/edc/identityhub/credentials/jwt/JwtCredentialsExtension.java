/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.credentials.jwt;

import org.eclipse.edc.identityhub.spi.credentials.transformer.CredentialEnvelopeTransformerRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

/**
 * Extension for handling parsing and encoding verifiable credential in JWT format.
 */
@Extension(value = JwtCredentialsExtension.NAME)
public class JwtCredentialsExtension implements ServiceExtension {

    public static final String NAME = "Verifiable credential in JWT format";

    @Inject
    private CredentialEnvelopeTransformerRegistry transformerRegistry;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        transformerRegistry.register(new JwtCredentialEnvelopeTransformer(typeManager.getMapper()));
    }
}
