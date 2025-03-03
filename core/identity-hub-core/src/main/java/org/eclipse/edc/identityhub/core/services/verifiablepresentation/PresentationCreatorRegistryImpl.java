/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *       Cofinity-X - Improvements for VC DataModel 2.0
 *
 */

package org.eclipse.edc.identityhub.core.services.verifiablepresentation;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.PresentationCreatorRegistry;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.PresentationGenerator;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.identityhub.core.services.verifiablepresentation.generators.PresentationGeneratorConstants.CONTROLLER_ADDITIONAL_DATA;
import static org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource.queryByParticipantContextId;

public class PresentationCreatorRegistryImpl implements PresentationCreatorRegistry {

    private final Map<CredentialFormat, PresentationGenerator<?>> creators = new HashMap<>();
    private final KeyPairService keyPairService;
    private final ParticipantContextService participantContextService;
    private final TransactionContext transactionContext;

    public PresentationCreatorRegistryImpl(KeyPairService keyPairService, ParticipantContextService participantContextService, TransactionContext transactionContext) {
        this.keyPairService = keyPairService;
        this.participantContextService = participantContextService;
        this.transactionContext = transactionContext;
    }

    @Override
    public void addCreator(PresentationGenerator<?> creator, CredentialFormat format) {
        creators.put(format, creator);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T createPresentation(String participantContextId, List<VerifiableCredentialContainer> credentials, CredentialFormat format, Map<String, Object> additionalData) {
        var creator = ofNullable(creators.get(format)).orElseThrow(() -> new EdcException("No %s was found for CredentialFormat %s".formatted(PresentationGenerator.class.getSimpleName(), format)));

        var query = queryByParticipantContextId(participantContextId)
                .filter(new Criterion("state", "=", KeyPairState.ACTIVATED.code()))
                .build();

        return transactionContext.execute(() -> {
            var keyPairResult = keyPairService.query(query)
                    .orElseThrow(f -> new EdcException("Error obtaining private key for participant '%s': %s".formatted(participantContextId, f.getFailureDetail())));

            // check if there is a default key pair
            var keyPair = keyPairResult.stream().filter(KeyPairResource::isDefaultPair).findAny()
                    .orElseGet(() -> keyPairResult.stream().findFirst().orElse(null));

            if (keyPair == null) {
                throw new EdcException("No active key pair found for participant '%s'".formatted(participantContextId));
            }

            var did = participantContextService.getParticipantContext(participantContextId)
                    .map(ParticipantContext::getDid)
                    .orElseThrow(f -> new EdcException(f.getFailureDetail()));

            var additionalDataWithController = new HashMap<>(additionalData);
            additionalDataWithController.put(CONTROLLER_ADDITIONAL_DATA, did);

            return (T) creator.generatePresentation(credentials, keyPair.getPrivateKeyAlias(), keyPair.getKeyId(), did, additionalDataWithController);
        });
    }
}
