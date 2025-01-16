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

package org.eclipse.edc.identityhub.core.services.query;

import org.eclipse.edc.iam.identitytrust.spi.model.PresentationQueryMessage;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.identityhub.spi.transformation.ScopeToCriterionTransformer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.resolution.CredentialQueryResolver;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.resolution.QueryResult;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
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
import java.util.stream.Stream;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;


public class CredentialQueryResolverImpl implements CredentialQueryResolver {

    private final CredentialStore credentialStore;
    private final ScopeToCriterionTransformer scopeTransformer;
    private final RevocationServiceRegistry revocationServiceRegistry;
    private final Monitor monitor;

    public CredentialQueryResolverImpl(CredentialStore credentialStore, ScopeToCriterionTransformer scopeTransformer, RevocationServiceRegistry revocationServiceRegistry, Monitor monitor) {
        this.credentialStore = credentialStore;
        this.scopeTransformer = scopeTransformer;
        this.revocationServiceRegistry = revocationServiceRegistry;
        this.monitor = monitor;
    }

    @Override
    public QueryResult query(String participantContextId, PresentationQueryMessage query, List<String> accessTokenScopes) {
        if (query.getPresentationDefinition() != null) {
            throw new UnsupportedOperationException("Querying with a DIF Presentation Exchange definition is not yet supported.");
        }
        var requestedScopes = query.getScopes();
        // check that all access token scopes are valid
        var accessTokenScopesParseResult = parseScopes(accessTokenScopes);
        if (accessTokenScopesParseResult.failed()) {
            return QueryResult.invalidScope(accessTokenScopesParseResult.getFailureMessages());
        }

        // fetch all credentials according to the scopes in the access token:
        var allowedScopes = accessTokenScopesParseResult.getContent();

        if (allowedScopes.isEmpty()) {
            // no scopes granted, no scopes requested, return empty list
            if (requestedScopes.isEmpty()) {
                return QueryResult.success(Stream.empty());
            }
            // no scopes granted, but some requested -> unauthorized! This is a shortcut to save some database communication
            var msg = "Permission was not granted on any credentials (empty access token scope list), but %d were requested.".formatted(requestedScopes.size());
            monitor.warning(msg);
            QueryResult.unauthorized(msg.formatted(requestedScopes.size()));
        }
        var allowedCred = queryCredentials(allowedScopes, participantContextId);
        if (allowedCred.failed()) {
            return QueryResult.storageFailure(allowedCred.getFailureMessages());
        }

        var allowedCredentials = allowedCred.getContent();
        Stream<VerifiableCredentialResource> credentialResult;

        // the client did not request any scopes, so we simply return all they have access to
        if (requestedScopes.isEmpty()) {
            credentialResult = allowedCredentials.stream();
        } else {
            // check that all prover scopes are valid
            var requestedScopesParseResult = parseScopes(requestedScopes);
            if (requestedScopesParseResult.failed()) {
                return QueryResult.invalidScope(requestedScopesParseResult.getFailureMessages());
            }
            // query storage for requested credentials
            var requestedCredentialResult = queryCredentials(requestedScopesParseResult.getContent(), participantContextId);
            if (requestedCredentialResult.failed()) {
                return QueryResult.storageFailure(requestedCredentialResult.getFailureMessages());
            }
            var requestedCredentials = requestedCredentialResult.getContent();

            // clients can never request more credentials than they are permitted to, i.e. their scope list can not exceed the scopes taken
            // from the access token
            var isValidQuery = new HashSet<>(allowedCredentials.stream().map(VerifiableCredentialResource::getId).toList())
                    .containsAll(requestedCredentials.stream().map(VerifiableCredentialResource::getId).toList());

            if (!isValidQuery) {
                return QueryResult.unauthorized("Invalid query: requested Credentials outside of scope.");
            }

            credentialResult = requestedCredentials.stream();
        }
        // filter out any expired, revoked or suspended credentials
        return QueryResult.success(credentialResult
                .filter(this::filterInvalidCredentials)
                .map(VerifiableCredentialResource::getVerifiableCredential));
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
        var revocationResult = (credentialStatus == null || credentialStatus.isEmpty()) ? Result.success() : revocationServiceRegistry.checkValidity(credential);
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
        var filterByParticipant = new Criterion("participantContextId", "=", participantContextId);
        var filterNotRevoked = new Criterion("state", "!=", VcStatus.REVOKED.code());
        var filterNotExpired = new Criterion("state", "!=", VcStatus.EXPIRED.code());
        return QuerySpec.Builder.newInstance()
                .filter(List.of(criteria, filterByParticipant, filterNotRevoked, filterNotExpired))
                .build();
    }

}
