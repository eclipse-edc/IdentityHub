/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.defaults.store;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.query.ReflectionPropertyLookup;

import java.util.Collection;
import java.util.stream.Collectors;


/**
 * This class performs the lookup of properties in a {@link CredentialDefinition}.
 */
public class CredentialDefinitionLookup extends ReflectionPropertyLookup {
    @SuppressWarnings("unchecked")
    @Override
    public Object getProperty(String key, Object object) {
        var fieldValue = super.getProperty(key, object);
        if (key.equals("formats")) {
            Collection<CredentialFormat> formats = (Collection<CredentialFormat>) fieldValue;
            return formats.stream().map(CredentialFormat::name).collect(Collectors.toList());
        }
        return fieldValue;
    }

}
