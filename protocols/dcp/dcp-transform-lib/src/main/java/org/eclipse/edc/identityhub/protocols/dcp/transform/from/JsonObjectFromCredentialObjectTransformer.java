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
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_BINDING_METHODS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_ISSUANCE_POLICY_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_OFFER_REASON_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_PROFILE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_TERM;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromCredentialObjectTransformer extends AbstractNamespaceAwareJsonLdTransformer<CredentialObject, JsonObject> {

    private final TypeManager typeManager;
    private final String typeContext;

    private final JsonLdNamespace xsdNamespace = new JsonLdNamespace("http://www.w3.org/2001/XMLSchema#");

    public JsonObjectFromCredentialObjectTransformer(TypeManager typeManager, String typeContext, JsonLdNamespace namespace) {
        super(CredentialObject.class, JsonObject.class, namespace);
        this.typeManager = typeManager;
        this.typeContext = typeContext;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull CredentialObject credentialObject, @NotNull TransformerContext transformerContext) {

        var issuancePolicy = typeManager.getMapper(typeContext).convertValue(credentialObject.getIssuancePolicy(), JsonObject.class);

        var issuancePolicyJson = Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add(JsonLdKeywords.TYPE, JsonLdKeywords.JSON)
                        .add(JsonLdKeywords.VALUE, issuancePolicy))
                .build();


        var p = createValue(credentialObject.getProfile(), xsdNamespace.toIri("string"));

        var bindingMethods = credentialObject.getBindingMethods().stream()
                .map(bindingMethod -> createValue(bindingMethod, xsdNamespace.toIri("string")))
                .collect(toJsonArray());

        return Json.createObjectBuilder()
                .add(ID, credentialObject.getId())
                .add(TYPE, forNamespace(CREDENTIAL_OBJECT_TERM))
                .add(forNamespace(CREDENTIAL_OBJECT_OFFER_REASON_TERM), createValue(credentialObject.getOfferReason(), xsdNamespace.toIri("string")))
                .add(forNamespace(CREDENTIAL_OBJECT_PROFILE_TERM), p)
                .add(forNamespace(CREDENTIAL_OBJECT_BINDING_METHODS_TERM), bindingMethods)
                .add(forNamespace(CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM), credentialObject.getCredentialType())
                .add(forNamespace(CREDENTIAL_OBJECT_ISSUANCE_POLICY_TERM), issuancePolicyJson)
                .build();
    }


    private JsonObject createValue(String value, String type) {
        return Json.createObjectBuilder()
                .add(JsonLdKeywords.TYPE, type)
                .add(JsonLdKeywords.VALUE, value)
                .build();
    }
}
