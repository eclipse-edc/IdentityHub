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

import org.eclipse.edc.identityhub.spi.KeyPairService;
import org.eclipse.edc.identityhub.spi.generator.PresentationCreatorRegistry;
import org.eclipse.edc.identityhub.spi.generator.PresentationGenerator;
import org.eclipse.edc.identityhub.spi.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.model.ParticipantResource;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.VerifiableCredentialContainer;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.Criterion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;

public class PresentationCreatorRegistryImpl implements PresentationCreatorRegistry {

    private final Map<CredentialFormat, PresentationGenerator<?>> creators = new HashMap<>();
    private final KeyPairService keyPairService;

    public PresentationCreatorRegistryImpl(KeyPairService keyPairService) {
        this.keyPairService = keyPairService;
    }

    @Override
    public void addCreator(PresentationGenerator<?> creator, CredentialFormat format) {
        creators.put(format, creator);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T createPresentation(String participantContextId, List<VerifiableCredentialContainer> credentials, CredentialFormat format, Map<String, Object> additionalData) {
        var creator = ofNullable(creators.get(format)).orElseThrow(() -> new EdcException("No PresentationCreator was found for CredentialFormat %s".formatted(format)));

        var query = ParticipantResource.queryByParticipantId(participantContextId)
                .filter(new Criterion("state", "=", KeyPairState.ACTIVE.code()))
                .build();

        var keyPairResult = keyPairService.query(query)
                .orElseThrow(f -> new EdcException("Error obtaining private key for participant '%s': %s".formatted(participantContextId, f.getFailureDetail())));

        // check if there is a default key pair
        var keyPair = keyPairResult.stream().filter(KeyPairResource::isDefaultPair).findAny()
                .orElseGet(() -> keyPairResult.stream().findFirst().orElse(null));

        if (keyPair == null) {
            throw new EdcException("No active key pair found for participant '%s'".formatted(participantContextId));
        }

        return (T) creator.generatePresentation(credentials, keyPair.getKeyId(), additionalData);
    }
}
