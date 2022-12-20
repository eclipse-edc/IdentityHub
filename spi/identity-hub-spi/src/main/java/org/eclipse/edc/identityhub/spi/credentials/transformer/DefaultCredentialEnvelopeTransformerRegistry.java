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

public class DefaultCredentialEnvelopeTransformerRegistry implements CredentialEnvelopeTransformerRegistry {


    private final Map<String, CredentialEnvelopeTransformer> validators = new HashMap<>();


    public DefaultCredentialEnvelopeTransformerRegistry() {

    }

    @Override
    public void register(CredentialEnvelopeTransformer validator) {
        validators.put(validator.dataFormat(), validator);
    }

    @Override
    public CredentialEnvelopeTransformer resolve(String dataFormat) {
        return validators.get(dataFormat);
    }
}
