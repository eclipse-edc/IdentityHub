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

import java.util.HashMap;
import java.util.Map;

public class CredentialEnvelopeTransformerRegistryImpl implements CredentialEnvelopeTransformerRegistry {


    private final Map<String, CredentialEnvelopeTransformer> transformers = new HashMap<>();


    @Override
    public void register(CredentialEnvelopeTransformer transformer) {
        transformers.put(transformer.dataFormat(), transformer);
    }

    @Override
    public CredentialEnvelopeTransformer resolve(String dataFormat) {
        return transformers.get(dataFormat);
    }
}
