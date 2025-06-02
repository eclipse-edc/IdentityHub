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

package org.eclipse.edc.identityhub.core.services.verifiablecredential;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.CredentialWriteRequest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.CredentialWriter;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.ISSUED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTED;
import static org.eclipse.edc.spi.result.ServiceResult.from;
import static org.eclipse.edc.spi.result.ServiceResult.success;


public class CredentialWriterImpl implements CredentialWriter {
    private static final List<String> VALID_CREDENTIAL_FORMATS = Arrays.stream(CredentialFormat.values()).map(Object::toString).toList();
    private static final List<HolderRequestState> ALLOWED_STATES = List.of(REQUESTED, ISSUED);
    private final CredentialStore credentialStore;
    private final TypeTransformerRegistry credentialTransformerRegistry;
    private final TransactionContext transactionContext;
    private final ObjectMapper objectMapper;
    private final HolderCredentialRequestStore holderCredentialRequestStore;

    public CredentialWriterImpl(CredentialStore credentialStore, TypeTransformerRegistry credentialTransformerRegistry, TransactionContext transactionContext, ObjectMapper objectMapper, HolderCredentialRequestStore holderCredentialRequestStore) {
        this.credentialStore = credentialStore;
        this.credentialTransformerRegistry = credentialTransformerRegistry;
        this.transactionContext = transactionContext;
        this.objectMapper = objectMapper;
        this.holderCredentialRequestStore = holderCredentialRequestStore;
    }

    @Override
    public ServiceResult<Void> write(String holderPid, String issuerPid, Collection<CredentialWriteRequest> writeRequests, String participantContextId) {
        return transactionContext.execute(() -> {

            // get holder request
            var holderRequestResult = holderCredentialRequestStore.findByIdAndLease(holderPid);
            if (holderRequestResult.failed()) {
                return from(holderRequestResult).mapEmpty();
            }

            var holderRequest = holderRequestResult.getContent();
            if (!ALLOWED_STATES.contains(holderRequest.stateAsEnum())) {
                return ServiceResult.badRequest("HolderCredentialRequest is expected to be in any of the states '%s' but was '%s'".formatted(ALLOWED_STATES, holderRequest.stateAsString()));
            }

            // store actual credentials
            for (var writeRequest : writeRequests) { // use for loop to abort early: merging ServiceResults in a stream operation is not really possible
                var convertResult = convertToResource(writeRequest, participantContextId);
                if (convertResult.failed()) {
                    return convertResult.mapEmpty();
                }
                var resource = convertResult.getContent();

                // verify that the received credentials correspond to the credential request that was made prior
                var receivedCredential = resource.getVerifiableCredential();
                var receivedTypes = receivedCredential.credential().getType();
                var receivedFormat = receivedCredential.format().toString();

                // check if the list of originally requested credentials contains the received credential
                var requestedCredential = holderRequest.getIdsAndFormats().stream()
                        .filter(rqc -> receivedTypes.contains(rqc.credentialType()) && receivedFormat.equalsIgnoreCase(rqc.format()))
                        .findFirst();

                if (requestedCredential.isEmpty()) {
                    return ServiceResult.unauthorized("No credential request was made for Credentials of type '%s' serialized as '%s'".formatted(receivedTypes, receivedFormat));
                }

                // store the credential object ID for later use, e.g. automatic re-issuance
                resource.getMetadata().put("credentialObjectId", requestedCredential.get().id());

                var createResult = credentialStore.create(resource);

                if (createResult.failed()) {
                    return from(createResult);
                }
            }

            //update holder request
            holderRequest.transitionIssued(issuerPid);
            holderCredentialRequestStore.save(holderRequest);

            return success();
        });
    }

    private ServiceResult<VerifiableCredentialResource> convertToResource(CredentialWriteRequest credentialWriteRequest, String participantContextId) {

        CredentialFormat credentialFormat;
        try {
            credentialFormat = CredentialFormat.valueOf(credentialWriteRequest.credentialFormat().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ServiceResult.badRequest(String.format("Invalid format: '%s', expected one of %s".formatted(credentialWriteRequest.credentialFormat(), VALID_CREDENTIAL_FORMATS)));
        }

        //attempt to convert the raw credential to JSON -> would mean LD, or JWT otherwise
        var transformationResult = tryConvertToJson(credentialWriteRequest.rawCredential())
                .map(jsonObjectCredential -> Result.success(objectMapper.convertValue(jsonObjectCredential, VerifiableCredential.class)))
                .orElseGet(() -> credentialTransformerRegistry.transform(credentialWriteRequest.rawCredential(), VerifiableCredential.class));

        if (transformationResult.failed()) {
            return ServiceResult.unexpected(transformationResult.getFailureDetail());
        }
        var credential = transformationResult.getContent();

        var container = new VerifiableCredentialContainer(credentialWriteRequest.rawCredential(), credentialFormat, credential);

        var resource = VerifiableCredentialResource.Builder.newInstance()
                .credential(container)
                .id(credential.getId())
                .state(VcStatus.ISSUED)
                .holderId(extractHolder(credential))
                .issuerId(credential.getIssuer().id())
                .timestamp(Instant.now().toEpochMilli())
                .participantContextId(participantContextId)
                .build();

        return ServiceResult.success(resource);
    }

    private Optional<JsonObject> tryConvertToJson(@NotNull String rawCredential) {
        try {
            return Optional.of(objectMapper.readValue(rawCredential, JsonObject.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private String extractHolder(VerifiableCredential credential) {
        return credential.getCredentialSubject().stream().findFirst().map(CredentialSubject::getId).orElse(null);
    }
}
