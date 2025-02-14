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
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus.CREDENTIAL_REQUEST_REQUEST_ID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus.CREDENTIAL_REQUEST_STATUS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus.CREDENTIAL_REQUEST_TERM;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromCredentialRequestStatusTransformer extends AbstractNamespaceAwareJsonLdTransformer<CredentialRequestStatus, JsonObject> {

    public JsonObjectFromCredentialRequestStatusTransformer(JsonLdNamespace namespace) {
        super(CredentialRequestStatus.class, JsonObject.class, namespace);
    }

    @Override
    public @Nullable JsonObject transform(@NotNull CredentialRequestStatus credentialRequestStatus, @NotNull TransformerContext transformerContext) {

        return Json.createObjectBuilder()
                .add(TYPE, forNamespace(CREDENTIAL_REQUEST_TERM))
                .add(forNamespace(CREDENTIAL_REQUEST_REQUEST_ID_TERM), credentialRequestStatus.getRequestId())
                .add(forNamespace(CREDENTIAL_REQUEST_STATUS_TERM), credentialRequestStatus.getStatus().name())
                .build();
    }
}
