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

import org.eclipse.edc.identityhub.spi.ScopeToCriterionTransformer;
import org.eclipse.edc.identityhub.spi.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.resolution.CredentialQueryResolver;
import org.eclipse.edc.identityhub.spi.resolution.QueryResult;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identitytrust.model.credentialservice.PresentationQueryMessage;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;


public class CredentialQueryResolverImpl implements CredentialQueryResolver {

    private final CredentialStore credentialStore;
    private final ScopeToCriterionTransformer scopeTransformer;

    public CredentialQueryResolverImpl(CredentialStore credentialStore, ScopeToCriterionTransformer scopeTransformer) {
        this.credentialStore = credentialStore;
        this.scopeTransformer = scopeTransformer;
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

        return isValidQuery ?
                QueryResult.success(requestedCredentials.stream().map(VerifiableCredentialResource::getVerifiableCredential))
                : QueryResult.unauthorized("Invalid query: requested Credentials outside of scope.");
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
        return QuerySpec.Builder.newInstance()
                .filter(List.of(criteria, filterByParticipant))
                .build();
    }

}
