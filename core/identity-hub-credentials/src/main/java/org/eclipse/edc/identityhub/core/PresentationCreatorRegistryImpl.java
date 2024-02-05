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

package org.eclipse.edc.identityhub.core;

import org.eclipse.edc.identityhub.spi.generator.PresentationCreatorRegistry;
import org.eclipse.edc.identityhub.spi.generator.PresentationGenerator;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.VerifiableCredentialContainer;
import org.eclipse.edc.spi.EdcException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;

public class PresentationCreatorRegistryImpl implements PresentationCreatorRegistry {

    private final Map<CredentialFormat, PresentationGenerator<?>> creators = new HashMap<>();
    private final Map<String, CredentialFormat> keyIds = new HashMap<>();

    @Override
    public void addCreator(PresentationGenerator<?> creator, CredentialFormat format) {
        creators.put(format, creator);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T createPresentation(List<VerifiableCredentialContainer> credentials, CredentialFormat format, Map<String, Object> additionalData) {
        var creator = ofNullable(creators.get(format)).orElseThrow(() -> new EdcException("No PresentationCreator was found for CredentialFormat %s".formatted(format)));

        var keyid = additionalData.getOrDefault("keyId", null);
        if (keyid == null) {
            throw new EdcException("No key ID was specified when creating presentation. 'additionalData' must contain an entry 'keyId' containing the fully-qualified key ID.");
        }

        var keyId = ofNullable(keyIds.get(format)).orElseThrow(() -> new EdcException("No key ID was registered for CredentialFormat %s".formatted(format)));

        return (T) creator.generatePresentation(credentials, null, additionalData);
    }

    @Override
    public void addKeyId(String keyId, CredentialFormat format) {
        keyIds.put(keyId, format);
    }

}
