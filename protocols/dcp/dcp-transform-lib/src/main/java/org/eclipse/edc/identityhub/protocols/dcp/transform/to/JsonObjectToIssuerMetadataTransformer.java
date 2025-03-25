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

package org.eclipse.edc.identityhub.protocols.dcp.transform.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.IssuerMetadata;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class JsonObjectToIssuerMetadataTransformer extends AbstractNamespaceAwareJsonLdTransformer<JsonObject, IssuerMetadata> {


    public JsonObjectToIssuerMetadataTransformer(JsonLdNamespace namespace) {
        super(JsonObject.class, IssuerMetadata.class, namespace);
    }

    @Override
    public @Nullable IssuerMetadata transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext transformerContext) {

        var issuerMetadata = IssuerMetadata.Builder.newInstance();

        Optional.ofNullable(jsonObject.get(IssuerMetadata.ISSUER_METADATA_ISSUER_IRI))
                .map(issuer -> transformString(issuer, transformerContext))
                .map(issuerMetadata::issuer);

        Optional.ofNullable(jsonObject.get(forNamespace(IssuerMetadata.ISSUER_METADATA_CREDENTIALS_SUPPORTED_TERM)))
                .map(issuer -> transformArray(issuer, CredentialObject.class, transformerContext))
                .map(issuerMetadata::credentialsSupported);

        return issuerMetadata.build();
    }

}
