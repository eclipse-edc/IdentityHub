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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.presentationdefinition.PresentationDefinition;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.Builder;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_BINDING_METHODS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_ISSUANCE_POLICY_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_OFFER_REASON_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_PROFILE_TERM;

public class JsonObjectToCredentialObjectTransformer extends AbstractNamespaceAwareJsonLdTransformer<JsonObject, CredentialObject> {

    private final TypeManager typeManager;
    private final String typeContext;

    public JsonObjectToCredentialObjectTransformer(TypeManager typeManager, String typeContext, JsonLdNamespace namespace) {
        super(JsonObject.class, CredentialObject.class, namespace);
        this.typeManager = typeManager;
        this.typeContext = typeContext;
    }

    @Override
    public @Nullable CredentialObject transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext transformerContext) {

        var credentialObject = Builder.newInstance();
        var issuancePolicy = jsonObject.get(forNamespace(CREDENTIAL_OBJECT_ISSUANCE_POLICY_TERM));
        if (issuancePolicy != null) {
            Optional.ofNullable(readIssuancePolicy(issuancePolicy, transformerContext))
                    .map(credentialObject::issuancePolicy);
        }

        credentialObject.id(nodeId(jsonObject));

        Optional.ofNullable(jsonObject.get(forNamespace(CREDENTIAL_OBJECT_OFFER_REASON_TERM)))
                .map(offerReason -> transformString(offerReason, transformerContext))
                .ifPresent(credentialObject::offerReason);

        Optional.ofNullable(jsonObject.get(forNamespace(CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM)))
                .map(credentialType -> transformString(credentialType, transformerContext))
                .ifPresent(credentialObject::credentialType);

        Optional.ofNullable(jsonObject.get(forNamespace(CREDENTIAL_OBJECT_PROFILE_TERM)))
                .ifPresent(credentialType -> transformArrayOrObject(credentialType, Object.class, (obj) -> credentialObject.profile(obj.toString()), transformerContext));


        Optional.ofNullable(jsonObject.get(forNamespace(CREDENTIAL_OBJECT_BINDING_METHODS_TERM)))
                .ifPresent(credentialType -> transformArrayOrObject(credentialType, Object.class, (obj) -> credentialObject.bindingMethod(obj.toString()), transformerContext));


        return credentialObject.build();
    }

    private PresentationDefinition readIssuancePolicy(JsonValue v, TransformerContext context) {
        var rawJson = getPresentationObject(v);
        try {
            return typeManager.getMapper(typeContext).readValue(rawJson.toString(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            context.reportProblem("Error reading JSON literal: %s".formatted(e.getMessage()));
            return null;
        }
    }

    private JsonObject getPresentationObject(JsonValue jsonValue) {
        JsonObject jsonObject;
        if (jsonValue.getValueType() == JsonValue.ValueType.ARRAY && !((JsonArray) jsonValue).isEmpty()) {
            jsonObject = jsonValue.asJsonArray().getJsonObject(0);
        } else {
            jsonObject = jsonValue.asJsonObject();
        }
        return jsonObject.getJsonObject(JsonLdKeywords.VALUE);
    }

}
