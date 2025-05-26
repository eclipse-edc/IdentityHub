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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestSpecifier;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage.CREDENTIAL_REQUEST_MESSAGE_CREDENTIALS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestMessage.CREDENTIAL_REQUEST_MESSAGE_HOLDER_PID_TERM;

public class JsonObjectToCredentialRequestMessageTransformer extends AbstractNamespaceAwareJsonLdTransformer<JsonObject, CredentialRequestMessage> {

    private final TypeManager typeManager;
    private final String typeContext;

    public JsonObjectToCredentialRequestMessageTransformer(TypeManager typeManager, String typeContext, JsonLdNamespace namespace) {
        super(JsonObject.class, CredentialRequestMessage.class, namespace);
        this.typeManager = typeManager;
        this.typeContext = typeContext;
    }

    @Override
    public @Nullable CredentialRequestMessage transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext transformerContext) {

        var requestMessage = CredentialRequestMessage.Builder.newInstance();
        var credentials = jsonObject.get(forNamespace(CREDENTIAL_REQUEST_MESSAGE_CREDENTIALS_TERM));
        if (credentials != null) {
            Optional.ofNullable(readCredentialRequests(credentials, transformerContext))
                    .map(requestMessage::credentials);
        }
        requestMessage.holderPid(transformString(jsonObject.get(forNamespace(CREDENTIAL_REQUEST_MESSAGE_HOLDER_PID_TERM)), transformerContext));
        return requestMessage.build();
    }

    private List<CredentialRequestSpecifier> readCredentialRequests(JsonValue v, TransformerContext context) {
        var rawJson = getCredentials(v);
        try {
            return typeManager.getMapper(typeContext).readValue(rawJson.toString(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            context.reportProblem("Error reading JSON literal: %s".formatted(e.getMessage()));
            return null;
        }
    }

    private JsonArray getCredentials(JsonValue jsonValue) {
        JsonObject jsonObject;
        if (jsonValue.getValueType() == JsonValue.ValueType.ARRAY && !((JsonArray) jsonValue).isEmpty()) {
            jsonObject = jsonValue.asJsonArray().getJsonObject(0);
        } else {
            jsonObject = jsonValue.asJsonObject();
        }
        return jsonObject.getJsonArray(JsonLdKeywords.VALUE);
    }

}
