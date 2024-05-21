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
 *
 */

package org.eclipse.edc.identityhub.query;

import org.eclipse.edc.iam.identitytrust.spi.model.PresentationQueryMessage;
import org.eclipse.edc.iam.verifiablecredentials.spi.RevocationListService;
import org.eclipse.edc.identityhub.spi.ScopeToCriterionTransformer;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.resolution.CredentialQueryResolver;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.resolution.QueryResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;


public class CredentialQueryResolverImpl implements CredentialQueryResolver {

    private final CredentialStore credentialStore;
    private final ScopeToCriterionTransformer scopeTransformer;
    private final RevocationListService revocationService;
    private final Monitor monitor;

    public CredentialQueryResolverImpl(CredentialStore credentialStore, ScopeToCriterionTransformer scopeTransformer, RevocationListService revocationService, Monitor monitor) {
        this.credentialStore = credentialStore;
        this.scopeTransformer = scopeTransformer;
        this.revocationService = revocationService;
        this.monitor = monitor;
    }

    @Override
    public QueryResult query(String participantContextId, PresentationQueryMessage query, List<String> issuerScopes) {
        if (query.getPresentationDefinition() != null) {
            throw new UnsupportedOperationException("Querying with a DIF Presentation Exchange definition is not yet supported.");
        }
        if (query.getScopes().isEmpty()) {
            return QueryResult.noScopeFound("Invalid query: must contain at least one scope.");
        }

        // check that all prover scopes are valid
        var proverScopeResult = parseScopes(query.getScopes());
        if (proverScopeResult.failed()) {
            return QueryResult.invalidScope(proverScopeResult.getFailureMessages());
        }

        // check that all issuer scopes are valid
        var issuerScopeResult = parseScopes(issuerScopes);
        if (issuerScopeResult.failed()) {
            return QueryResult.invalidScope(issuerScopeResult.getFailureMessages());
        }

        // query storage for requested credentials
        var credentialResult = queryCredentials(proverScopeResult.getContent(), participantContextId);
        if (credentialResult.failed()) {
            return QueryResult.storageFailure(credentialResult.getFailureMessages());
        }

        // the credentials requested by the other party
        var requestedCredentials = credentialResult.getContent();

        // check that prover scope is not wider than issuer scope
        var allowedCred = queryCredentials(issuerScopeResult.getContent(), participantContextId);
        if (allowedCred.failed()) {
            return QueryResult.invalidScope(allowedCred.getFailureMessages());
        }

        // now narrow down the requested credentials to only contain allowed credentials
        var content = allowedCred.getContent();
        var isValidQuery = new HashSet<>(content).containsAll(requestedCredentials);

        // filter out any expired, revoked or suspended credentials
        return isValidQuery ?
                QueryResult.success(requestedCredentials.stream()
                        .filter(this::filterInvalidCredentials) // we still have to filter invalid creds, b/c a revocation may not have been detected yet
                        .map(VerifiableCredentialResource::getVerifiableCredential))
                : QueryResult.unauthorized("Invalid query: requested Credentials outside of scope.");
    }

    private boolean filterInvalidCredentials(VerifiableCredentialResource verifiableCredentialResource) {
        var now = Instant.now();
        var credential = verifiableCredentialResource.getVerifiableCredential().credential();
        // issuance date can not be null, due to builder validation
        if (credential.getIssuanceDate().isAfter(now)) {
            monitor.warning("Credential '%s' is not yet valid.".formatted(credential.getId()));
            return false;
        }
        if (credential.getExpirationDate() != null && credential.getExpirationDate().isBefore(now)) {
            monitor.warning("Credential '%s' is expired.".formatted(credential.getId()));
            return false;
        }
        var credentialStatus = credential.getCredentialStatus();
        var revocationResult = (credentialStatus == null || credentialStatus.isEmpty()) ? Result.success() : revocationService.checkValidity(credential);
        if (revocationResult.failed()) {
            monitor.warning("Credential '%s' not valid: %s".formatted(credential.getId(), revocationResult.getFailureDetail()));
            return false;
        }
        return true;
    }

    /**
     * Parses a list of scope strings, converts them to {@link Criterion} objects, and returns a {@link Result} containing
     * the list of converted criteria. If any scope string fails to be converted, a failure result is returned.
     *
     * @param scopes The list of scope strings to parse and convert.
     * @return A {@link Result} containing the list of converted {@link Criterion} objects.
     */
    private Result<List<Criterion>> parseScopes(List<String> scopes) {
        var transformResult = scopes.stream()
                .map(scopeTransformer::transform)
                .toList();

        if (transformResult.stream().anyMatch(AbstractResult::failed)) {
            return failure(transformResult.stream().flatMap(r -> r.getFailureMessages().stream()).toList());
        }
        return success(transformResult.stream().map(AbstractResult::getContent).toList());
    }


    private Result<Collection<VerifiableCredentialResource>> queryCredentials(List<Criterion> criteria, String participantContextId) {
        var results = criteria.stream()
                .map(criterion -> convertToQuerySpec(criterion, participantContextId))
                .map(credentialStore::query)
                .toList();

        if (results.stream().anyMatch(AbstractResult::failed)) {
            return Result.failure(results.stream().flatMap(r -> r.getFailureMessages().stream()).toList());
        }
        return Result.success(results.stream()
                .flatMap(result -> result.getContent().stream())
                .collect(Collectors.toList()));
    }

    private QuerySpec convertToQuerySpec(Criterion criteria, String participantContextId) {
        var filterByParticipant = new Criterion("participantId", "=", participantContextId);
        var filterNotRevoked = new Criterion("state", "!=", VcStatus.REVOKED.code());
        var filterNotExpired = new Criterion("state", "!=", VcStatus.EXPIRED.code());
        return QuerySpec.Builder.newInstance()
                .filter(List.of(criteria, filterByParticipant, filterNotRevoked, filterNotExpired))
                .build();
    }

}
