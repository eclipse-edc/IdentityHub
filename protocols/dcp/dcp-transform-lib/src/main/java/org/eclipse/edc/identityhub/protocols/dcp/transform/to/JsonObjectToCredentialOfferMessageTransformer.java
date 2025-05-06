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
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.CREDENTIALS_NAMESPACE_W3C;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage.CREDENTIALS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage.CREDENTIAL_ISSUER_TERM;

public class JsonObjectToCredentialOfferMessageTransformer extends AbstractNamespaceAwareJsonLdTransformer<JsonObject, CredentialOfferMessage> {
    public JsonObjectToCredentialOfferMessageTransformer(JsonLdNamespace namespace) {
        super(JsonObject.class, CredentialOfferMessage.class, namespace);
    }

    @Override
    public @Nullable CredentialOfferMessage transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext transformerContext) {
        var builder = CredentialOfferMessage.Builder.newInstance();

        builder.issuer(transformString(jsonObject.get(CREDENTIALS_NAMESPACE_W3C.toIri(CREDENTIAL_ISSUER_TERM)), transformerContext));

        var credentialsArray = jsonObject.getJsonArray(forNamespace(CREDENTIALS_TERM));

        if (credentialsArray != null) {
            var credentialObjects = transformArray(credentialsArray, CredentialObject.class, transformerContext);
            builder.credentials(credentialObjects);
        }
        return builder.build();
    }
}
