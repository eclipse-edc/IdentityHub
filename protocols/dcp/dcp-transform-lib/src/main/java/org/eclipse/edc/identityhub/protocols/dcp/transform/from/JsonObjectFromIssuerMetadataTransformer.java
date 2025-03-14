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

package org.eclipse.edc.identityhub.protocols.dcp.transform.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.IssuerMetadata;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.IssuerMetadata.ISSUER_METADATA_CREDENTIALS_SUPPORTED_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.IssuerMetadata.ISSUER_METADATA_ISSUER_IRI;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.IssuerMetadata.ISSUER_METADATA_TERM;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromIssuerMetadataTransformer extends AbstractNamespaceAwareJsonLdTransformer<IssuerMetadata, JsonObject> {


    private final JsonBuilderFactory factory = Json.createBuilderFactory(Map.of());

    public JsonObjectFromIssuerMetadataTransformer(JsonLdNamespace namespace) {
        super(IssuerMetadata.class, JsonObject.class, namespace);
    }

    @Override
    public @Nullable JsonObject transform(@NotNull IssuerMetadata issuerMetadata, @NotNull TransformerContext transformerContext) {

        var supportedCredentials = issuerMetadata.getCredentialsSupported().stream()
                .map(credentialObject -> transformerContext.transform(credentialObject, JsonObject.class))
                .collect(toJsonArray());

        return Json.createObjectBuilder()
                .add(TYPE, forNamespace(ISSUER_METADATA_TERM))
                .add(ISSUER_METADATA_ISSUER_IRI, createId(factory, issuerMetadata.getIssuer()))
                .add(forNamespace(ISSUER_METADATA_CREDENTIALS_SUPPORTED_TERM), supportedCredentials)
                .build();
    }

}
