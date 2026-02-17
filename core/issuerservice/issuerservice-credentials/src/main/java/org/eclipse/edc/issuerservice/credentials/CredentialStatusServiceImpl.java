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

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringConstants;
import org.eclipse.edc.issuerservice.spi.credentials.CredentialStatusService;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListInfo;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListInfoFactoryRegistry;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListManager;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.spi.result.ServiceResult.badRequest;
import static org.eclipse.edc.spi.result.ServiceResult.from;
import static org.eclipse.edc.spi.result.ServiceResult.success;
import static org.eclipse.edc.spi.result.ServiceResult.unexpected;

public class CredentialStatusServiceImpl implements CredentialStatusService {
    private final CredentialStore credentialStore;
    private final TransactionContext transactionContext;
    private final Monitor monitor;
    private final StatusListInfoFactoryRegistry statusListInfoFactoryRegistry;
    private final StatusListManager statusListManager;
    private final CredentialGeneratorRegistry credentialGeneratorRegistry;

    public CredentialStatusServiceImpl(CredentialStore credentialStore, TransactionContext transactionContext, Monitor monitor,
                                       StatusListInfoFactoryRegistry statusListInfoFactoryRegistry, StatusListManager statusListManager,
                                       CredentialGeneratorRegistry credentialGeneratorRegistry) {
        this.credentialStore = credentialStore;
        this.transactionContext = transactionContext;
        this.monitor = monitor;
        this.statusListInfoFactoryRegistry = statusListInfoFactoryRegistry;
        this.statusListManager = statusListManager;
        this.credentialGeneratorRegistry = credentialGeneratorRegistry;
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

            var result = getCredential(holderCredentialId)
                    .compose(this::getRevocationInfo);

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

            return updateStatusCredential(revocationInfo.statusListCredential())
                    .compose(updatedStatusListCredential -> getCredential(holderCredentialId)
                            .onSuccess(VerifiableCredentialResource::revoke)
                            .compose(userCredential -> update(updatedStatusListCredential, userCredential))
                    );
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

    private ServiceResult<Void> update(VerifiableCredentialResource... credentials) {
        return Arrays.stream(credentials).map(credentialStore::update)
                .reduce(StoreResult.success(), (a, b) -> a.compose(i -> b))
                .flatMap(ServiceResult::from);
    }

    private ServiceResult<VerifiableCredentialResource> getCredential(String credentialId) {
        return credentialStore.findById(credentialId).flatMap(ServiceResult::from);
    }

    /**
     * updates the status list credential with the new bitstring. For this, the status list credential is converted into
     * a JWT and signed with the private key.
     */
    private ServiceResult<VerifiableCredentialResource> updateStatusCredential(VerifiableCredentialResource credentialResource) {
        var verifiableCredential = credentialResource.getVerifiableCredential();

        return credentialGeneratorRegistry.signCredential(credentialResource.getParticipantContextId(), verifiableCredential.credential(), verifiableCredential.format())
                .flatMap(ServiceResult::from)
                .map(container -> credentialResource.toBuilder()
                        .credential(container)
                        .build());
    }

    private ServiceResult<StatusListInfo> getRevocationInfo(VerifiableCredentialResource resource) {
        var statusObjects = resource.getVerifiableCredential().credential().getCredentialStatus();

        var revocationStatus = statusObjects.stream()
                .filter(st -> st.additionalProperties().values().stream().anyMatch(v -> v.toString().endsWith(BitstringConstants.REVOCATION)))
                .findFirst();

        if (revocationStatus.isEmpty()) {
            return badRequest("Credential did not contain a credentialStatus object with 'statusPurpose = revocation'");
        }

        var status = revocationStatus.get();

        return ofNullable(statusListInfoFactoryRegistry.getInfoFactory(status.type()))
                .map(statusListInfoServiceResult -> statusListInfoServiceResult.create(status))
                .orElseGet(() -> badRequest("No StatusList implementation for type '%s' found.".formatted(status.type())));
    }

}
