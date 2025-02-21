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
import jakarta.json.JsonArray;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.CREDENTIALS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.CREDENTIAL_MESSAGE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.HOLDER_PID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.ISSUER_PID_TERM;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromCredentialMessageTransformer extends AbstractNamespaceAwareJsonLdTransformer<CredentialMessage, JsonObject> {

    private final TypeManager typeManager;
    private final String typeContext;
    private final JsonBuilderFactory factory;

    public JsonObjectFromCredentialMessageTransformer(JsonBuilderFactory factory, TypeManager typeManager, String typeContext, JsonLdNamespace namespace) {
        super(CredentialMessage.class, JsonObject.class, namespace);
        this.typeManager = typeManager;
        this.typeContext = typeContext;
        this.factory = factory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull CredentialMessage credentialRequestMessage, @NotNull TransformerContext transformerContext) {

        var credentials = typeManager.getMapper(typeContext).convertValue(credentialRequestMessage.getCredentials(), JsonArray.class);

        var jsonCredentials = Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add(JsonLdKeywords.TYPE, JsonLdKeywords.JSON)
                        .add(JsonLdKeywords.VALUE, credentials))
                .build();

        return Json.createObjectBuilder()
                .add(TYPE, forNamespace(CREDENTIAL_MESSAGE_TERM))
                .add(forNamespace(HOLDER_PID_TERM), createId(factory, credentialRequestMessage.getHolderPid()))
                .add(forNamespace(ISSUER_PID_TERM), createId(factory, credentialRequestMessage.getIssuerPid()))
                .add(forNamespace(CREDENTIALS_TERM), jsonCredentials)
                .build();
    }

}
