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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairResource;
import org.eclipse.edc.identityhub.spi.keypair.model.KeyPairState;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.issuerservice.spi.holder.HolderService;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGenerationRequest;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGenerator;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.mapping.IssuanceClaimsMapper;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.Result;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource.queryByParticipantContextId;
import static org.eclipse.edc.spi.result.Result.success;

public class CredentialGeneratorRegistryImpl implements CredentialGeneratorRegistry {


    private final Map<CredentialFormat, CredentialGenerator> generators = new HashMap<>();
    private final IssuanceClaimsMapper issuanceClaimsMapper;
    private final ParticipantContextService participantContextService;
    private final HolderService holderService;

    private final KeyPairService keyPairService;


    public CredentialGeneratorRegistryImpl(IssuanceClaimsMapper issuanceClaimsMapper, ParticipantContextService participantContextService, HolderService holderService, KeyPairService keyPairService) {
        this.issuanceClaimsMapper = issuanceClaimsMapper;
        this.participantContextService = participantContextService;
        this.holderService = holderService;
        this.keyPairService = keyPairService;
    }

    private static Supplier<Result<VerifiableCredentialContainer>> noCredentialFoundError(CredentialFormat format) {
        return () -> Result.failure("No generator found for format %s".formatted(format.toString()));
    }

    @Override
    public void addGenerator(CredentialFormat credentialFormat, CredentialGenerator credentialGenerator) {
        generators.put(credentialFormat, credentialGenerator);
    }

    @Override
    public Result<VerifiableCredentialContainer> generateCredential(String participantContextId, String participantId, CredentialGenerationRequest credentialGenerationRequest, Map<String, Object> claims) {

        return issuanceClaimsMapper.apply(credentialGenerationRequest.definition().getMappings(), claims)
                .compose(mappedClaims -> generateCredentialInternal(participantContextId, participantId, credentialGenerationRequest, mappedClaims));
    }

    @Override
    public Result<VerifiableCredentialContainer> signCredential(String participantContextId, VerifiableCredential credential, CredentialFormat format) {
        return ofNullable(generators.get(format))
                .map(generator -> fetchActiveKeyPair(participantContextId)
                        .compose(keyPairResource -> generator.signCredential(credential, keyPairResource.getPrivateKeyAlias(), keyPairResource.getKeyId()))
                        .compose(token -> success(new VerifiableCredentialContainer(token, format, credential))))
                .orElseGet(noCredentialFoundError(format));
    }

    private Result<KeyPairResource> fetchActiveKeyPair(String participantContextId) {
        var query = queryByParticipantContextId(participantContextId)
                .filter(new Criterion("state", "=", KeyPairState.ACTIVATED.code()))
                .build();


        var keyPairResult = keyPairService.query(query);
        if (keyPairResult.failed()) {
            return Result.failure("Error obtaining private key for participant '%s': %s".formatted(participantContextId, keyPairResult.getFailureDetail()));
        }

        var keyPairs = keyPairResult.getContent();
        // check if there is a default key pair
        var keyPair = keyPairs.stream().filter(KeyPairResource::isDefaultPair).findAny()
                .orElseGet(() -> keyPairs.stream().findFirst().orElse(null));

        if (keyPair == null) {
            return Result.failure("No active key pair found for participant '%s'".formatted(participantContextId));
        }

        return success(keyPair);

    }

    private Result<VerifiableCredentialContainer> generateCredentialInternal(String participantContextId, String participantId, CredentialGenerationRequest credentialGenerationRequest, Map<String, Object> mappedClaims) {
        return ofNullable(generators.get(credentialGenerationRequest.format()))
                .map(generator -> generateCredentialInternal(participantContextId, participantId, generator, credentialGenerationRequest.definition(), mappedClaims))
                .orElseGet(() -> Result.failure("No generator found for format %s".formatted(credentialGenerationRequest.format())));

    }

    private Result<VerifiableCredentialContainer> generateCredentialInternal(String participantContextId, String participantId, CredentialGenerator generator, CredentialDefinition definition, Map<String, Object> mappedClaims) {

        try {
            var issuerDid = participantContextService.getParticipantContext(participantContextId)
                    .map(ParticipantContext::getDid)
                    .orElseThrow(f -> new EdcException(f.getFailureDetail()));

            var participantDid = ofNullable(participantId)
                    .map(holderId -> holderService.findById(holderId).orElseThrow(f -> new EdcException(f.getFailureDetail())))
                    .map(Holder::getDid)
                    .orElse(issuerDid);

            return fetchActiveKeyPair(participantContextId)
                    .compose(keyPair -> generator.generateCredential(definition, keyPair.getPrivateKeyAlias(), keyPair.getKeyId(), issuerDid, participantDid, mappedClaims));
        } catch (EdcException e) {
            return Result.failure(e.getMessage());
        }

    }

}
