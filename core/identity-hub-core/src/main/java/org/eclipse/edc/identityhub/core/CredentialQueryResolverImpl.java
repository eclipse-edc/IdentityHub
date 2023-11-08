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
import org.eclipse.edc.identityhub.spi.model.PresentationQuery;
import org.eclipse.edc.identityhub.spi.resolution.CredentialQueryResolver;
import org.eclipse.edc.identityhub.spi.resolution.QueryResult;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.store.model.VerifiableCredentialResource;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

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
    public QueryResult query(PresentationQuery query, List<String> issuerScopes) {
        if (query.getPresentationDefinition() != null) {
            throw new UnsupportedOperationException("Querying with a DIF Presentation Exchange definition is not yet supported.");
        }
        if (query.getScopes().isEmpty()) {
            return QueryResult.noScopeFound("Invalid query: must contain at least one scope.");
        }

        // check that all prover scopes are valid
        var proverScopeResult = parseScopes(query.getScopes());
        if (proverScopeResult.failed()) return QueryResult.invalidScope(proverScopeResult.getFailureMessages());

        // check that all issuer scopes are valid
        var issuerScopeResult = parseScopes(issuerScopes);
        if (issuerScopeResult.failed()) return QueryResult.invalidScope(issuerScopeResult.getFailureMessages());

        // query storage for requested credentials
        var queryspec = convertToQuerySpec(proverScopeResult.getContent());
        var res = credentialStore.query(queryspec);
        if (res.failed()) {
            return QueryResult.storageFailure(res.getFailureMessages());
        }

        // the credentials requested by the other party
        var requestedCredentials = res.getContent().toList();

        // check that prover scope is not wider than issuer scope
        var issuerQuery = convertToQuerySpec(issuerScopeResult.getContent());
        var predicate = issuerQuery.getFilterExpression().stream()
                .map(c -> credentialsPredicate(c.getOperandRight().toString()))
                .reduce(Predicate::or)
                .orElse(x -> false);

        // now narrow down the requested credentials to only contain allowed creds
        var allowedCredentials = requestedCredentials.stream().filter(predicate).toList();

        var isValidQuery = validateResults(new ArrayList<>(requestedCredentials), new ArrayList<>(allowedCredentials));

        return isValidQuery ?
                QueryResult.success(requestedCredentials.stream().map(VerifiableCredentialResource::getVerifiableCredential))
                : QueryResult.unauthorized("Invalid query: requested Credentials outside of scope.");
    }

    /**
     * Returns a predicate that filters {@link VerifiableCredentialResource} objects based on the provided type by
     * inspecting the {@code types} property of the {@link org.eclipse.edc.identitytrust.model.VerifiableCredential} that is
     * encapsulated in the resource.
     *
     * @param type The type to filter by.
     * @return A predicate that filters {@link VerifiableCredentialResource} objects based on the provided type.
     */
    private Predicate<VerifiableCredentialResource> credentialsPredicate(String type) {
        return resource -> {
            var cred = resource.getVerifiableCredential();
            return cred != null && cred.credential() != null && cred.credential().getTypes().contains(type);
        };
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

    /**
     * Checks whether the list of requested credentials is valid. Validity is determined by whether the list of requested credentials
     * contains elements that are not in the list of allowed credentials. The list of allowed credentials may contain more elements, but not less.
     * Every element, that is in the list of requested credentials must be found in the list of allowed credentials.
     *
     * @param requestedCredentials The list of requested credentials.
     * @param allowedCredentials   The list of allowed credentials.
     * @return true if the list of requested credentials contains only elements that can be found in the list of allowed credentials, false otherwise.
     */
    private boolean validateResults(List<VerifiableCredentialResource> requestedCredentials, List<VerifiableCredentialResource> allowedCredentials) {
        if (requestedCredentials == allowedCredentials) {
            return true;
        }
        if (requestedCredentials.size() > allowedCredentials.size()) {
            return false;
        }

        requestedCredentials.removeAll(allowedCredentials);
        return requestedCredentials.isEmpty();
    }

    private QuerySpec convertToQuerySpec(List<Criterion> criteria) {
        return QuerySpec.Builder.newInstance()
                .filter(criteria)
                .build();
    }
}
