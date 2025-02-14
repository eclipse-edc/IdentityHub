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

package org.eclipse.edc.identityhub.protocols.dcp.issuer.transform.from;

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

import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_BINDING_METHODS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_ISSUANCE_POLICY_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_OFFER_REASON_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_PROFILES_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_TERM;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromCredentialObjectTransformer extends AbstractNamespaceAwareJsonLdTransformer<CredentialObject, JsonObject> {

    private final TypeManager typeManager;
    private final String typeContext;

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

        return Json.createObjectBuilder()
                .add(TYPE, forNamespace(CREDENTIAL_OBJECT_TERM))
                .add(forNamespace(CREDENTIAL_OBJECT_OFFER_REASON_TERM), credentialObject.getOfferReason())
                .add(forNamespace(CREDENTIAL_OBJECT_PROFILES_TERM), Json.createArrayBuilder(credentialObject.getProfiles()))
                .add(forNamespace(CREDENTIAL_OBJECT_BINDING_METHODS_TERM), Json.createArrayBuilder(credentialObject.getBindingMethods()))
                .add(forNamespace(CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM), credentialObject.getCredentialType())
                .add(forNamespace(CREDENTIAL_OBJECT_ISSUANCE_POLICY_TERM), issuancePolicyJson)
                .build();
    }

}
