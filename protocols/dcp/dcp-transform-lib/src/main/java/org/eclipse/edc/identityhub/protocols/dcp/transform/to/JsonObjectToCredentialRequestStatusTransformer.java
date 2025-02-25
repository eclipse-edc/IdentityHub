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
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus.CREDENTIAL_REQUEST_HOLDER_PID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus.CREDENTIAL_REQUEST_ISSUER_PID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialRequestStatus.CREDENTIAL_REQUEST_STATUS_TERM;

public class JsonObjectToCredentialRequestStatusTransformer extends AbstractNamespaceAwareJsonLdTransformer<JsonObject, CredentialRequestStatus> {

    private final String ns;

    public JsonObjectToCredentialRequestStatusTransformer(JsonLdNamespace namespace) {
        super(JsonObject.class, CredentialRequestStatus.class, namespace);
        ns = namespace.namespace();

    }

    @Override
    public @Nullable CredentialRequestStatus transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext transformerContext) {
        var issuerPid = transformString(jsonObject.get(forNamespace(CREDENTIAL_REQUEST_ISSUER_PID_TERM)), transformerContext);
        var holderPid = transformString(jsonObject.get(forNamespace(CREDENTIAL_REQUEST_HOLDER_PID_TERM)), transformerContext);
        var status = transformString(jsonObject.get(forNamespace(CREDENTIAL_REQUEST_STATUS_TERM)), transformerContext);
        return CredentialRequestStatus.Builder.newInstance()
                .issuerPid(issuerPid)
                .holderPid(holderPid)
                .status(toStatus(status))
                .build();
    }
    
    private CredentialRequestStatus.Status toStatus(String status) {
        return Optional.ofNullable(status)
                .map(s -> CredentialRequestStatus.Status.valueOf(s.replace(ns, "")))
                .orElse(null);
    }

}
