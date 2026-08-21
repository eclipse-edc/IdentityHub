/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.api.validation;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.CREDENTIALS_NAMESPACE_W3C;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_BINDING_METHODS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_ISSUANCE_POLICY_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_OFFER_REASON_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject.CREDENTIAL_OBJECT_PROFILE_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage.CREDENTIALS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage.CREDENTIAL_ISSUER_TERM;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class CredentialOfferMessageValidatorTest {
    private final CredentialOfferMessageValidator validator = new CredentialOfferMessageValidator();

    @Test
    void validate_missingIssuer() {
        var msg = Json.createObjectBuilder()
                .add(JsonLdKeywords.TYPE, "CredentialOfferMessage")
                .build();

        assertThat(validator.validate(msg)).isFailed()
                .detail().contains("Invalid format: must contain a 'issuer' property.");
    }

    @Test
    void validate_withCredentials_success() {
        var msg = Json.createObjectBuilder()
                .add(CREDENTIALS_NAMESPACE_W3C.toIri(CREDENTIAL_ISSUER_TERM), "test-issuer")
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIALS_TERM), Json.createArrayBuilder()
                        .add(createCredentialObject())
                        .add(createCredentialObject()))
                .build();
        assertThat(validator.validate(msg)).isSucceeded();
    }

    @Test
    void validate_withCredentials_withViolation() {
        var msg = Json.createObjectBuilder()
                .add(CREDENTIALS_NAMESPACE_W3C.toIri(CREDENTIAL_ISSUER_TERM), "test-issuer")
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIALS_TERM), Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("invalid-key", "invalid-value").build()))
                .build();
        assertThat(validator.validate(msg)).isFailed();
    }

    // A4.8: CredentialOfferMessage with an EMPTY credentials array -> validation failure (spec requires a non-empty array; currently passes)
    @Test
    @DisplayName("A4.8: a credential offer message with an empty credentials array fails validation")
    void validate_emptyCredentialsArray_shouldFail() {
        // arrange
        var msg = Json.createObjectBuilder()
                .add(CREDENTIALS_NAMESPACE_W3C.toIri(CREDENTIAL_ISSUER_TERM), "test-issuer")
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIALS_TERM), Json.createArrayBuilder())
                .build();

        // act + assert: the spec requires a non-empty credentials array
        assertThat(validator.validate(msg)).isFailed();
    }

    @Test
    @DisplayName("A4.8: a credential offer message with a null credentials array fails validation")
    void validate_nullCredentialsArray_shouldFail() {
        // must use map, JsonObject does not allow null values
        var payload = new HashMap<String, String>();
        payload.put(CREDENTIALS_NAMESPACE_W3C.toIri(CREDENTIAL_ISSUER_TERM), "test-issuer");
        payload.put(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIALS_TERM), null);

        assertThat(validator.validate(Json.createObjectBuilder(payload).build())).isFailed();

        var payloadNoCredentials = Map.of(CREDENTIALS_NAMESPACE_W3C.toIri(CREDENTIAL_ISSUER_TERM), "test-issuer");
        assertThat(validator.validate(Json.createObjectBuilder(payloadNoCredentials).build())).isFailed();
    }

    private JsonObject createCredentialObject() {
        return Json.createObjectBuilder()
                .add(JsonLdKeywords.ID, UUID.randomUUID().toString())
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OBJECT_ISSUANCE_POLICY_TERM), Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add(JsonLdKeywords.TYPE, JsonLdKeywords.JSON)
                                .add(JsonLdKeywords.VALUE, Json.createObjectBuilder()
                                        .add("id", "test-id")
                                        .build())))
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OBJECT_PROFILE_TERM), Json.createArrayBuilder(List.of("profile")))
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OBJECT_OFFER_REASON_TERM), "offerReason")
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OBJECT_CREDENTIAL_TYPE_TERM), "MembershipCredential")
                .add(DSPACE_DCP_NAMESPACE_V_1_0.toIri(CREDENTIAL_OBJECT_BINDING_METHODS_TERM), Json.createArrayBuilder(List.of("binding")))
                .build();
    }
}