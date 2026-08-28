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
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.CredentialWriteRequest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.CredentialWriter;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialProfile;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.ISSUED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTED;
import static org.eclipse.edc.spi.result.ServiceResult.from;
import static org.eclipse.edc.spi.result.ServiceResult.success;


public class CredentialWriterImpl implements CredentialWriter {

    private static final List<HolderRequestState> ALLOWED_STATES = List.of(REQUESTED, ISSUED);
    private final CredentialStore credentialStore;
    private final TypeTransformerRegistry credentialTransformerRegistry;
    private final TransactionContext transactionContext;
    private final ObjectMapper objectMapper;
    private final HolderCredentialRequestStore holderCredentialRequestStore;
    private final TokenValidationService tokenValidationService;
    private final DidPublicKeyResolver publicKeyResolver;
    private final Monitor monitor;

    public CredentialWriterImpl(CredentialStore credentialStore, TypeTransformerRegistry credentialTransformerRegistry, TransactionContext transactionContext, ObjectMapper objectMapper,
                                HolderCredentialRequestStore holderCredentialRequestStore, TokenValidationService tokenValidationService, DidPublicKeyResolver publicKeyResolver,
                                Monitor monitor) {
        this.monitor = monitor;
        this.credentialStore = credentialStore;
        this.credentialTransformerRegistry = credentialTransformerRegistry;
        this.transactionContext = transactionContext;
        this.objectMapper = objectMapper;
        this.holderCredentialRequestStore = holderCredentialRequestStore;
        this.tokenValidationService = tokenValidationService;
        this.publicKeyResolver = publicKeyResolver;
    }

    @Override
    public ServiceResult<Void> write(String holderPid, String holderDid, String issuerPid, String issuerDid, Collection<CredentialWriteRequest> writeRequests, String participantContextId) {
        return transactionContext.execute(() -> {

            // get holder request
            var holderRequestResult = holderCredentialRequestStore.findByIdAndLease(holderPid);
            if (holderRequestResult.failed()) {
                return from(holderRequestResult).mapEmpty();
            }

            var holderRequest = holderRequestResult.getContent();

            var result = writeCredentials(holderRequest, holderPid, holderDid, issuerPid, issuerDid, writeRequests, participantContextId);
            if (result.failed()) {
                // the request remains open for another delivery attempt, so it must not stay leased
                holderCredentialRequestStore.breakLease(holderRequest);
            }
            return result;
        });
    }

    private ServiceResult<Void> writeCredentials(HolderCredentialRequest holderRequest, String holderPid, String holderDid, String issuerPid, String issuerDid,
                                                 Collection<CredentialWriteRequest> writeRequests, String participantContextId) {
        // requests of other participant contexts are not writable here, and their existence must not be observable either
        if (!holderRequest.getParticipantContextId().equals(participantContextId)) {
            return ServiceResult.notFound("HolderCredentialRequest with ID '%s' does not exist".formatted(holderPid));
        }

        // credentials are only accepted from the Issuer the request was addressed to: having asked that Issuer for them
        // is what makes it trusted for this request
        if (!holderRequest.getIssuerDid().equals(issuerDid)) {
            return ServiceResult.unauthorized("HolderCredentialRequest '%s' was sent to Issuer '%s', so credentials delivered by '%s' are not accepted"
                    .formatted(holderPid, holderRequest.getIssuerDid(), issuerDid));
        }

        if (!ALLOWED_STATES.contains(holderRequest.stateAsEnum())) {
            return ServiceResult.badRequest("HolderCredentialRequest is expected to be in any of the states '%s' but was '%s'".formatted(ALLOWED_STATES, holderRequest.stateAsString()));
        }

        // a message without credentials is accepted as a no-op: nothing was issued, so the request stays as it is and
        // waits for the credentials to arrive. Saving it unchanged releases the lease acquired above.
        if (writeRequests.isEmpty()) {
            holderCredentialRequestStore.save(holderRequest);
            return success();
        }

        // credentials for this request were already stored, so this is the Issuer re-sending them, e.g. after an
        // ambiguous outcome of its earlier delivery. Accept it, but do not store a second copy. Nothing is written here,
        // so the checks guarding the write below are not needed.
        if (holderRequest.stateAsEnum() == ISSUED) {
            // nothing changes about the request, but the lease acquired above must not be held on to
            holderCredentialRequestStore.breakLease(holderRequest);
            return success();
        }

        // once the Issuer's process ID is known it is fixed for this request, so a message reporting a different one
        // belongs to a different issuance and must not silently take over this request
        var knownIssuerPid = holderRequest.getIssuerPid();
        if (knownIssuerPid != null && !knownIssuerPid.isBlank() && !knownIssuerPid.equals(issuerPid)) {
            return ServiceResult.badRequest("HolderCredentialRequest '%s' is tracked under issuerPid '%s', but the message reported '%s'"
                    .formatted(holderPid, knownIssuerPid, issuerPid));
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

            // convert received format to a CredentialFormat
            var receivedFormat = CredentialProfile.formatForProfile(writeRequest.credentialFormat());
            if (receivedFormat.failed()) {
                return receivedFormat.mapFailure();
            }

            // only credentials that actually carry the Issuer's signature are stored
            var proofResult = verifyProof(writeRequest.rawCredential(), receivedFormat.getContent());
            if (proofResult.failed()) {
                monitor.warning("Rejecting a credential delivered for request '%s': %s".formatted(holderPid, proofResult.getFailureDetail()));
                return ServiceResult.badRequest("Could not verify the credential's proof: %s".formatted(proofResult.getFailureDetail()));
            }

            // check if the list of originally requested credentials contains the received credential
            var requestedCredential = holderRequest.getIdsAndFormats().stream()
                    .filter(rqc -> receivedTypes.contains(rqc.credentialType()))
                    // for compatibility, we need to convert both to a CredentialFormat and compare that:
                    .filter(rqc -> {
                        var requestedFormat = CredentialProfile.formatForProfile(rqc.format());
                        if (requestedFormat.failed()) {
                            return false;
                        }
                        return requestedFormat.getContent().equals(receivedFormat.getContent());
                    })
                    .findFirst();

            if (requestedCredential.isEmpty()) {
                return ServiceResult.unauthorized("No credential request was made for Credentials of type '%s' serialized as '%s'".formatted(receivedTypes, receivedFormat.getContent()));
            }

            // a credential is only usable by the Holder it was issued to, so every subject must be bound to its DID
            var boundToHolder = receivedCredential.credential().getCredentialSubject().stream()
                    .map(CredentialSubject::getId)
                    .allMatch(holderDid::equals);
            if (!boundToHolder) {
                return ServiceResult.unauthorized("Not all credentialSubject.id entries match the holder's DID");
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
    }

    /**
     * Verifies that a credential was actually signed by its Issuer. Token-based credentials are verified against the key
     * material resolved from the Issuer's DID document. Credentials carrying an embedded Linked-Data proof are not
     * verified here, as that requires a different verification suite.
     */
    private Result<Void> verifyProof(String rawCredential, CredentialFormat format) {
        if (format != CredentialFormat.VC1_0_JWT && format != CredentialFormat.VC2_0_JOSE) {
            monitor.warning("Received a credential in format '%s', which is stored without verifying its proof. Only '%s' and '%s' credentials are verified."
                    .formatted(format, CredentialFormat.VC1_0_JWT, CredentialFormat.VC2_0_JOSE));
            return Result.success();
        }
        var result = tokenValidationService.validate(rawCredential, publicKeyResolver, List.of());
        return result.failed() ? Result.failure(result.getFailureDetail()) : Result.success();
    }

    private ServiceResult<VerifiableCredentialResource> convertToResource(CredentialWriteRequest credentialWriteRequest, String participantContextId) {

        var profile = credentialWriteRequest.credentialFormat();
        var mappedFormat = CredentialProfile.formatForProfile(profile);

        if (mappedFormat.failed()) {
            return mappedFormat.mapFailure();
        }

        //attempt to convert the raw credential to JSON -> would mean LD, or JWT otherwise
        var transformationResult = tryConvertToJson(credentialWriteRequest.rawCredential())
                .map(jsonObjectCredential -> Result.success(objectMapper.convertValue(jsonObjectCredential, VerifiableCredential.class)))
                .orElseGet(() -> credentialTransformerRegistry.transform(credentialWriteRequest.rawCredential(), VerifiableCredential.class));

        if (transformationResult.failed()) {
            return ServiceResult.unexpected(transformationResult.getFailureDetail());
        }
        var credential = transformationResult.getContent();

        var container = new VerifiableCredentialContainer(credentialWriteRequest.rawCredential(), mappedFormat.getContent(), credential);

        var resource = VerifiableCredentialResource.Builder.newHolder()
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
