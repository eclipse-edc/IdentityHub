/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.api.credentialoffer;

import jakarta.json.Json;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.identityhub.http.IssuerEndpoints;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.IssuerMetadata;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.CredentialRequestManager;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.DCP_SCOPE_V_1_0;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Completes sparse {@link CredentialObject} entries of an incoming {@code CredentialOfferMessage}.
 * <p>
 * Credential entries of an offer may be sparse, i.e. carry only their {@code id}. All other properties must then be
 * taken from the {@code credentialsSupported} list served by the Issuer's Metadata API.
 */
public class CredentialObjectResolver {

    private static final String METADATA_PATH = "/metadata";

    private final DidResolverRegistry didResolverRegistry;
    private final EdcHttpClient httpClient;
    private final JsonLd jsonLd;
    private final TypeTransformerRegistry transformerRegistry;
    private final Monitor monitor;

    public CredentialObjectResolver(DidResolverRegistry didResolverRegistry, EdcHttpClient httpClient, JsonLd jsonLd, TypeTransformerRegistry transformerRegistry, Monitor monitor) {
        this.didResolverRegistry = didResolverRegistry;
        this.httpClient = httpClient;
        this.jsonLd = jsonLd;
        this.transformerRegistry = transformerRegistry;
        this.monitor = monitor;
    }

    /**
     * Returns the offered credential objects with every sparse entry replaced by its counterpart from the Issuer's
     * metadata. Offers without sparse entries are returned unchanged, in which case no metadata is fetched.
     *
     * @param issuerDid         the DID of the Issuer that sent the offer
     * @param credentialObjects the credential objects as received in the offer
     */
    public Result<List<CredentialObject>> resolve(String issuerDid, List<CredentialObject> credentialObjects) {
        if (credentialObjects.stream().noneMatch(CredentialObjectResolver::isSparse)) {
            return success(credentialObjects);
        }
        var metadata = fetchIssuerMetadata(issuerDid);
        if (metadata.failed()) {
            // the failure may describe the outcome of a request to whatever endpoint the Issuer's DID document names,
            // which is not for the caller to see
            monitor.debug("Issuer metadata of '%s' could not be fetched: %s".formatted(issuerDid, metadata.getFailureDetail()));
            return failure("The Issuer's metadata could not be fetched");
        }
        return complete(credentialObjects, metadata.getContent());
    }

    private static boolean isSparse(CredentialObject credentialObject) {
        return credentialObject.getCredentialType() == null || credentialObject.getCredentialType().isBlank();
    }

    private Result<List<CredentialObject>> complete(List<CredentialObject> credentialObjects, IssuerMetadata metadata) {
        var resolved = new ArrayList<CredentialObject>();
        for (var credentialObject : credentialObjects) {
            if (!isSparse(credentialObject)) {
                resolved.add(credentialObject);
                continue;
            }
            var supported = metadata.getCredentialsSupported().stream()
                    .filter(co -> Objects.equals(co.getId(), credentialObject.getId()))
                    .findFirst();
            if (supported.isEmpty()) {
                return failure("The Issuer's metadata does not contain a CredentialObject with ID '%s'".formatted(credentialObject.getId()));
            }
            resolved.add(supported.get());
        }
        return success(resolved);
    }

    private Result<IssuerMetadata> fetchIssuerMetadata(String issuerDid) {
        return getIssuerServiceEndpoint(issuerDid)
                .compose(endpoint -> httpClient.execute(new Request.Builder().url(endpoint + METADATA_PATH).get().build(), this::mapMetadata));
    }

    /**
     * Extracts the Issuer Service endpoint from the Issuer's DID document.
     */
    private Result<String> getIssuerServiceEndpoint(String issuerDid) {
        return didResolverRegistry.resolve(issuerDid)
                .compose(didDocument -> {
                    var service = didDocument.getService().stream()
                            .filter(s -> s.getType().equalsIgnoreCase(CredentialRequestManager.ISSUER_SERVICE_ENDPOINT_TYPE)).findAny();
                    return service.map(s -> success(s.getServiceEndpoint()))
                            .orElseGet(() -> failure("The Issuer's DID Document does not contain any '%s' endpoint"
                                    .formatted(CredentialRequestManager.ISSUER_SERVICE_ENDPOINT_TYPE)));
                });
    }

    private Result<IssuerMetadata> mapMetadata(Response response) {
        try (var body = response.body()) {
            if (!response.isSuccessful()) {
                return failure("Error fetching Issuer metadata: code: '%s', message: '%s'".formatted(response.code(), response.message()));
            }
            try (var reader = Json.createReader(new StringReader(IssuerEndpoints.readBody(body)))) {
                return jsonLd.expand(reader.readObject())
                        .compose(expanded -> transformerRegistry.forContext(DCP_SCOPE_V_1_0).transform(expanded, IssuerMetadata.class));
            }
        } catch (IOException e) {
            return failure("Error fetching Issuer metadata: %s".formatted(e.getMessage()));
        }
    }
}
