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
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialContainer;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.CREDENTIALS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.HOLDER_PID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.ISSUER_PID_TERM;


public class JsonObjectToCredentialMessageTransformer extends AbstractNamespaceAwareJsonLdTransformer<JsonObject, CredentialMessage> {

    private final TypeManager typeManager;
    private final String typeContext;

    public JsonObjectToCredentialMessageTransformer(TypeManager typeManager, String typeContext, JsonLdNamespace namespace) {
        super(JsonObject.class, CredentialMessage.class, namespace);
        this.typeManager = typeManager;
        this.typeContext = typeContext;
    }

    @Override
    public @Nullable CredentialMessage transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext transformerContext) {

        var requestMessage = CredentialMessage.Builder.newInstance();
        var credentials = jsonObject.get(forNamespace(CREDENTIALS_TERM));
        var issuerPid = transformString(jsonObject.get(forNamespace(ISSUER_PID_TERM)), transformerContext);
        requestMessage.issuerPid(issuerPid);

        var holderPid = transformString(jsonObject.get(forNamespace(HOLDER_PID_TERM)), transformerContext);
        requestMessage.holderPid(holderPid);

        var status = transformString(jsonObject.get(forNamespace(CredentialMessage.STATUS_TERM)), transformerContext);
        requestMessage.status(status);

        if (credentials != null) {
            ofNullable(readCredentialContainers(credentials, transformerContext))
                    .map(requestMessage::credentials);
        }
        return requestMessage.build();
    }

    private List<CredentialContainer> readCredentialContainers(JsonValue v, TransformerContext context) {
        var rawJson = getCredentials(v);
        if (rawJson.isEmpty()) {
            return Collections.emptyList();
        }
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
        if (jsonValue.getValueType() == JsonValue.ValueType.ARRAY) {
            if (((JsonArray) jsonValue).isEmpty()) {
                return JsonArray.EMPTY_JSON_ARRAY;
            }
            jsonObject = jsonValue.asJsonArray().getJsonObject(0);
        } else {
            jsonObject = jsonValue.asJsonObject();
        }
        return jsonObject.getJsonArray(JsonLdKeywords.VALUE);
    }

}
