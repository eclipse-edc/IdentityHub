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

package org.eclipse.edc.identityhub.spi.processor.data;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

/**
 * Contains a set of validators that will be applied based on dataFormat when receiving a CollectionWrite request
 */

@ExtensionPoint
public interface DataValidatorRegistry {

    /**
     * Register a {@link DataValidator} into the registry, and it will be associated to {@link DataValidator#dataFormat} Media type
     *
     * @param validator The validator
     */
    void register(DataValidator validator);


    /**
     * Returns a validator associated to the input dateFormat. If not present returns null
     *
     * @param dataFormat The input dataFormat
     * @return {@link DataValidator}
     */
    DataValidator resolve(String dataFormat);
}
