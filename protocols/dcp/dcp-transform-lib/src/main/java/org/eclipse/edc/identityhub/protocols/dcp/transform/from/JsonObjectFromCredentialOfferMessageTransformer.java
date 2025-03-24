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
import jakarta.json.JsonObject;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromCredentialOfferMessageTransformer extends AbstractNamespaceAwareJsonLdTransformer<CredentialOfferMessage, JsonObject> {
    public JsonObjectFromCredentialOfferMessageTransformer(JsonLdNamespace namespace) {
        super(CredentialOfferMessage.class, JsonObject.class, namespace);
    }

    @Override
    public @Nullable JsonObject transform(@NotNull CredentialOfferMessage credentialOfferMessage, @NotNull TransformerContext transformerContext) {
        var credentials = credentialOfferMessage.getCredentials().stream()
                .map(credentialObject -> transformerContext.transform(credentialObject, JsonObject.class))
                .collect(toJsonArray());

        return Json.createObjectBuilder()
                .add(TYPE, forNamespace(CredentialOfferMessage.CREDENTIAL_OFFER_MESSAGE_TERM))
                .add(forNamespace(CredentialOfferMessage.CREDENTIAL_ISSUER_TERM), credentialOfferMessage.getIssuer())
                .add(forNamespace(CredentialOfferMessage.CREDENTIALS_TERM), credentials)
                .build();
    }
}
