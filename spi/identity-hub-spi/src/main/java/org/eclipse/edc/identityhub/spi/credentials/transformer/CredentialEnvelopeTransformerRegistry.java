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

package org.eclipse.edc.identityhub.spi.credentials.transformer;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.jetbrains.annotations.Nullable;

/**
 * Contains a set of validators that will be applied based on dataFormat when receiving a CollectionWrite request
 */

@ExtensionPoint
public interface CredentialEnvelopeTransformerRegistry {

    /**
     * Register a {@link CredentialEnvelopeTransformer} into the registry, and it will be associated to {@link CredentialEnvelopeTransformer#dataFormat} Media type
     *
     * @param validator The validator
     */
    void register(CredentialEnvelopeTransformer validator);


    /**
     * Returns a validator associated to the input dateFormat. If not present returns null
     *
     * @param dataFormat The input dataFormat
     * @return {@link CredentialEnvelopeTransformer}
     */
    @Nullable
    CredentialEnvelopeTransformer resolve(String dataFormat);
}
