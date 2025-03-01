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

package org.eclipse.edc.issuerservice.issuance.generator;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGenerationRequest;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGenerator;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.mapping.IssuanceClaimsMapper;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.participant.ParticipantService;
import org.eclipse.edc.issuerservice.spi.participant.model.Participant;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CredentialGeneratorRegistryImpl implements CredentialGeneratorRegistry {


    private final Map<CredentialFormat, CredentialGenerator> generators = new HashMap<>();
    private final IssuanceClaimsMapper issuanceClaimsMapper;
    private final ParticipantContextService participantContextService;
    private final ParticipantService participantService;

    private final KeyPairService keyPairService;


    public CredentialGeneratorRegistryImpl(IssuanceClaimsMapper issuanceClaimsMapper, ParticipantContextService participantContextService, ParticipantService participantService, KeyPairService keyPairService) {
        this.issuanceClaimsMapper = issuanceClaimsMapper;
        this.participantContextService = participantContextService;
        this.participantService = participantService;
        this.keyPairService = keyPairService;
    }

    @Override
    public void addGenerator(CredentialFormat credentialFormat, CredentialGenerator credentialGenerator) {
        generators.put(credentialFormat, credentialGenerator);
    }

    @Override
    public Result<VerifiableCredentialContainer> generateCredential(String issuerContextId, String participantId, CredentialGenerationRequest credentialGenerationRequest, Map<String, Object> claims) {

        return issuanceClaimsMapper.apply(credentialGenerationRequest.definition().getMappings(), claims)
                .compose(mappedClaims -> generateCredentialInternal(issuerContextId, participantId, credentialGenerationRequest, mappedClaims));
    }


    private Result<KeyPairResource> fetchActiveKeyPair(String issuerContextId) {
        var query = ParticipantResource.queryByParticipantContextId(issuerContextId)
                .filter(new Criterion("state", "=", KeyPairState.ACTIVATED.code()))
                .build();


        var keyPairResult = keyPairService.query(query)
                .orElseThrow(f -> new EdcException("Error obtaining private key for participant '%s': %s".formatted(issuerContextId, f.getFailureDetail())));

        // check if there is a default key pair
        var keyPair = keyPairResult.stream().filter(KeyPairResource::isDefaultPair).findAny()
                .orElseGet(() -> keyPairResult.stream().findFirst().orElse(null));

        if (keyPair == null) {
            return Result.failure("No active key pair found for participant '%s'".formatted(issuerContextId));
        }

        return Result.success(keyPair);

    }
    
    private Result<VerifiableCredentialContainer> generateCredentialInternal(String participantContextId, String participantId, CredentialGenerationRequest credentialGenerationRequest, Map<String, Object> mappedClaims) {
        return Optional.ofNullable(generators.get(credentialGenerationRequest.format()))
                .map(generator -> generateCredentialInternal(participantContextId, participantId, generator, credentialGenerationRequest.definition(), mappedClaims))
                .orElseGet(() -> Result.failure("No generator found for format %s".formatted(credentialGenerationRequest.format())));

    }

    private Result<VerifiableCredentialContainer> generateCredentialInternal(String participantContextId, String participantId, CredentialGenerator generator, CredentialDefinition definition, Map<String, Object> mappedClaims) {

        try {
            var issuerDid = participantContextService.getParticipantContext(participantContextId)
                    .map(ParticipantContext::getDid)
                    .orElseThrow(f -> new EdcException(f.getFailureDetail()));

            var participantDid = participantService.findById(participantId)
                    .map(Participant::did)
                    .orElseThrow(f -> new EdcException(f.getFailureDetail()));

            return fetchActiveKeyPair(participantContextId)
                    .compose(keyPair -> generator.generateCredential(definition, keyPair.getPrivateKeyAlias(), keyPair.getKeyId(), issuerDid, participantDid, mappedClaims));
        } catch (EdcException e) {
            return Result.failure(e.getMessage());
        }

    }

}
