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

import org.eclipse.edc.identityhub.spi.model.PresentationQuery;
import org.eclipse.edc.identityhub.spi.resolution.CredentialQueryResolver;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.store.model.VerifiableCredentialResource;
import org.eclipse.edc.identitytrust.model.VerifiableCredentialContainer;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;


public class CredentialQueryResolverImpl implements CredentialQueryResolver {

    private static final String SCOPE_SEPARATOR = ":";
    private final CredentialStore credentialStore;
    private final List<String> allowedOperations = List.of("read", "*", "all");

    public CredentialQueryResolverImpl(CredentialStore credentialStore) {
        this.credentialStore = credentialStore;
    }

    @Override
    public Result<Stream<VerifiableCredentialContainer>> query(PresentationQuery query, List<String> issuerScopes) {
        if (query.getPresentationDefinition() != null) {
            throw new UnsupportedOperationException("Querying with a DIF Presentation Exchange definition is not yet supported.");
        }
        if (query.getScopes().isEmpty()) {
            return failure("Invalid query: must contain at least one scope.");
        }

        // check that all prover scopes are valid
        var proverScopeFailures = checkScope(query.getScopes());
        if (proverScopeFailures != null) return proverScopeFailures;

        // check that all issuer scopes are valid
        var issuerScopeFailures = checkScope(issuerScopes);
        if (issuerScopeFailures != null) return issuerScopeFailures;

        // query storage for requested credentials
        var queryspec = convertToQuerySpec(query.getScopes());
        var res = credentialStore.query(queryspec);
        if (res.failed()) {
            return failure(res.getFailureMessages());
        }

        // the credentials requested by the other party
        var wantedCredentials = res.getContent().toList();

        // check that prover scope is not wider than issuer scope
        var issuerQuery = convertToQuerySpec(issuerScopes);
        var predicate = issuerQuery.getFilterExpression().stream()
                .map(c -> credentialsPredicate(c.getOperandRight().toString()))
                .reduce(Predicate::or)
                .orElse(x -> false);

        // now narrow down the requested credentials to only contain allowed creds
        var allowedCredentials = wantedCredentials.stream().filter(predicate).toList();

        var isValidQuery = validateResults(new ArrayList<>(wantedCredentials), new ArrayList<>(allowedCredentials));

        return isValidQuery ?
                success(wantedCredentials.stream().map(VerifiableCredentialResource::getVerifiableCredential))
                : failure("Invalid query: requested Credentials outside of scope.");
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

    @Nullable
    private Result<Stream<VerifiableCredentialContainer>> checkScope(List<String> query) {
        var proverScopeFailures = query.stream()
                .map(this::isValidScope)
                .filter(AbstractResult::failed)
                .flatMap(r -> r.getFailureMessages().stream())
                .toList();
        if (!proverScopeFailures.isEmpty()) {
            return failure(proverScopeFailures);
        }
        return null;
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
        if (requestedCredentials.size() != allowedCredentials.size()) {
            return false;
        }

        requestedCredentials.removeAll(allowedCredentials);
        return requestedCredentials.isEmpty();
    }

    private QuerySpec convertToQuerySpec(List<String> scopes) {
        var criteria = scopes.stream()
                .map(this::convertScopeToCriterion)
                .toList();

        return QuerySpec.Builder.newInstance()
                .filter(criteria)
                .build();
    }

    /**
     * Converts a scope string to a {@link Criterion} object. For example,
     * <pre>
     *     org.eclipse.edc.vc.type:DemoCredential:read
     * </pre>
     * would be converted to
     * <pre>
     *     verifiableCredential.credential.types contains DemoCredential
     * </pre>
     * <p>
     * take note that the operation ("read") must be checked somewhere else, and is ignored here.
     *
     * @param scope The scope string to convert.
     * @return The converted {@link Criterion} object.
     */
    //todo: make this pluggable and more versatile
    private Criterion convertScopeToCriterion(String scope) {
        var tokens = isValidScope(scope);
        if (tokens.failed()) {
            throw new IllegalArgumentException("Scope string cannot be converted: %s".formatted(tokens.getFailureDetail()));
        }
        var credentialType = tokens.getContent()[1];
        return new Criterion("verifiableCredential.credential.types", "like", credentialType);
    }

    private Result<String[]> isValidScope(String scope) {
        if (scope == null) return failure("Scope was null");

        var tokens = scope.split(SCOPE_SEPARATOR);
        if (tokens.length != 3) {
            return failure("Scope string has invalid format.");
        }
        if (!allowedOperations.contains(tokens[2])) {
            return failure("Invalid scope operation: " + tokens[2]);
        }

        return success(tokens);
    }
}
