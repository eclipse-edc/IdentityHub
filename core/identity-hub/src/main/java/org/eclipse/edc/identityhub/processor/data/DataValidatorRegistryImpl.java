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

package org.eclipse.edc.identityhub.processor.data;

import org.eclipse.edc.identityhub.spi.processor.data.DataValidator;
import org.eclipse.edc.identityhub.spi.processor.data.DataValidatorRegistry;

import java.util.HashMap;
import java.util.Map;

public class DataValidatorRegistryImpl implements DataValidatorRegistry {


    private final Map<String, DataValidator> validators = new HashMap<>();


    public DataValidatorRegistryImpl() {

    }

    @Override
    public void register(DataValidator validator) {
        validators.put(validator.dataFormat(), validator);
    }

    @Override
    public DataValidator resolve(String dataFormat) {
        return validators.get(dataFormat);
    }
}
