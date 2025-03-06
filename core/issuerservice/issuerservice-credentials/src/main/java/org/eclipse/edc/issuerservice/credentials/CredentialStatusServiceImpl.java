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

package org.eclipse.edc.issuerservice.credentials;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringConstants;
import org.eclipse.edc.issuerservice.spi.credentials.CredentialStatusService;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListInfo;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListInfoFactoryRegistry;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListManager;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.spi.result.ServiceResult.badRequest;
import static org.eclipse.edc.spi.result.ServiceResult.from;
import static org.eclipse.edc.spi.result.ServiceResult.fromFailure;
import static org.eclipse.edc.spi.result.ServiceResult.success;
import static org.eclipse.edc.spi.result.ServiceResult.unexpected;

public class CredentialStatusServiceImpl implements CredentialStatusService {
    public static final TypeReference<Map<String, Object>> MAP_REF = new TypeReference<>() {
    };
    private final CredentialStore credentialStore;
    private final TransactionContext transactionContext;
    private final ObjectMapper objectMapper;
    private final Monitor monitor;
    private final TokenGenerationService tokenGenerationService;
    private final Supplier<String> privateKeyAlias;
    private final StatusListInfoFactoryRegistry statusListInfoFactoryRegistry;
    private final StatusListManager statusListManager;

    public CredentialStatusServiceImpl(CredentialStore credentialStore,
                                       TransactionContext transactionContext,
                                       ObjectMapper objectMapper,
                                       Monitor monitor,
                                       TokenGenerationService tokenGenerationService,
                                       Supplier<String> privateKeyAlias,
                                       StatusListInfoFactoryRegistry statusListInfoFactoryRegistry, StatusListManager statusListManager) {
        this.credentialStore = credentialStore;
        this.transactionContext = transactionContext;
        this.objectMapper = objectMapper.copy()
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY) // technically, credential subjects and credential status can be objects AND Arrays
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // e.g. @context
        this.monitor = monitor;
        this.tokenGenerationService = tokenGenerationService;
        this.privateKeyAlias = privateKeyAlias;
        this.statusListInfoFactoryRegistry = statusListInfoFactoryRegistry;
        this.statusListManager = statusListManager;
    }

    @Override
    public ServiceResult<VerifiableCredential> addCredential(String participantContextId, VerifiableCredential credential) {

        var entryResult = statusListManager.getActiveCredential(participantContextId);
        if (entryResult.failed()) {
            return entryResult.mapFailure();
        }

        var entry = entryResult.getContent();
        var cred = credential.toBuilder()
                .credentialStatus(entry.createCredentialStatus())
                .build();
        // update the status list: increment index, possible create new credential
        return statusListManager.incrementIndex(entry).compose(v -> success(cred));
    }

    @Override
    public ServiceResult<Void> revokeCredential(String holderCredentialId) {
        return transactionContext.execute(() -> {

            var credentialResult = getCredential(holderCredentialId);
            var result = credentialResult.compose(this::getRevocationInfo);

            if (result.failed()) {
                return result.mapFailure();
            }
            var revocationInfo = result.getContent();

            var status = revocationInfo.getStatus();
            if (status.failed()) {
                return result.mapFailure();
            }

            if (BitstringConstants.REVOCATION.equalsIgnoreCase(status.getContent())) {
                monitor.info("Revocation not necessary, credential is already revoked.");
                return success();
            }

            var setStatusResult = revocationInfo.setStatus(true);
            if (setStatusResult.failed()) {
                return unexpected(setStatusResult.getFailureDetail());
            }

            try {
                // update status credential
                var updatedRevocationCredential = updateStatusCredential(revocationInfo.statusListCredential());

                // update user credential
                var cred = credentialResult.getContent();
                cred.revoke();

                var merged = credentialStore.update(updatedRevocationCredential)
                        .compose(v -> credentialStore.update(cred));

                return from(merged);
            } catch (JOSEException e) {
                var msg = "Error signing BitstringStatusListCredential:  %s".formatted(e.getMessage());
                monitor.warning(msg, e);
                return unexpected(msg);
            }
        });
    }

    @Override
    public ServiceResult<Void> suspendCredential(String credentialId, @Nullable String reason) {
        throw new UnsupportedOperationException("Not supported by this implementation.");
    }

    @Override
    public ServiceResult<Void> resumeCredential(String credentialId, @Nullable String reason) {
        throw new UnsupportedOperationException("Not supported by this implementation.");
    }

    @Override
    public ServiceResult<String> getCredentialStatus(String credentialId) {
        return transactionContext.execute(() -> getCredential(credentialId)
                .compose(this::getRevocationInfo)
                .compose(r -> from(r.getStatus())));
    }

    @Override
    public ServiceResult<Collection<VerifiableCredentialResource>> queryCredentials(QuerySpec query) {
        return ServiceResult.from(credentialStore.query(query));
    }

    @Override
    public ServiceResult<VerifiableCredentialResource> getCredentialById(String credentialId) {
        return getCredential(credentialId);
    }

    private ServiceResult<VerifiableCredentialResource> getCredential(String credentialId) {
        // credential not found -> error
        var credentialResult = credentialStore.findById(credentialId);
        if (credentialResult.failed()) {
            return fromFailure(credentialResult);
        }
        // obtain the ID of the revocation list credential and the status index by reading the credential's status object, specifically
        // the one with "statusPurpose = revocation"
        var credential = credentialResult.getContent();
        return success(credential);
    }

    /**
     * updates the status list credential with the new bitstring. For this, the status list credential is converted into
     * a JWT and signed with the private key.
     */
    private VerifiableCredentialResource updateStatusCredential(VerifiableCredentialResource credentialResource) throws JOSEException {
        // encode credential as JWT
        var credential = credentialResource.getVerifiableCredential().credential();

        var claims = objectMapper.convertValue(credential, MAP_REF);
        var token = tokenGenerationService.generate(privateKeyAlias.get(), tokenParameters -> tokenParameters.claims(claims));

        var newJwt = token.getContent().getToken();

        var container = new VerifiableCredentialContainer(newJwt, credentialResource.getVerifiableCredential().format(), credential);

        return credentialResource.toBuilder()
                .credential(container)
                .build();
    }

    private ServiceResult<StatusListInfo> getRevocationInfo(VerifiableCredentialResource resource) {
        return getStatusInfo(resource, BitstringConstants.REVOCATION);
    }

    private ServiceResult<StatusListInfo> getStatusInfo(VerifiableCredentialResource holderCredential, String statusPurpose) {

        var statusObjects = holderCredential.getVerifiableCredential().credential().getCredentialStatus();

        var revocationStatus = statusObjects.stream()
                .filter(st -> st.additionalProperties().values().stream().anyMatch(v -> v.toString().endsWith(statusPurpose)))
                .findFirst();

        if (revocationStatus.isEmpty()) {
            return badRequest("Credential did not contain a credentialStatus object with 'statusPurpose = revocation'");
        }

        var status = revocationStatus.get();
        var revocationInfo = ofNullable(statusListInfoFactoryRegistry.getInfoFactory(status.type()))
                .map(cred -> cred.create(status));

        return revocationInfo.orElseGet(() -> badRequest("No StatusList implementation for type '%s' found.".formatted(status.type())));

    }

}
