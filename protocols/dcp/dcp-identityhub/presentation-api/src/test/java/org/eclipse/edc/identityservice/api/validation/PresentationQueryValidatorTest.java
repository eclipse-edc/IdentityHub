/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityservice.api.validation;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.identitytrust.spi.DcpConstants;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.presentationdefinition.Constraints;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.presentationdefinition.Field;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.presentationdefinition.InputDescriptor;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.presentationdefinition.PresentationDefinition;
import org.eclipse.edc.identityhub.api.validation.PresentationQueryValidator;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.iam.identitytrust.spi.model.PresentationQueryMessage.PRESENTATION_QUERY_MESSAGE_DEFINITION_TERM;
import static org.eclipse.edc.iam.identitytrust.spi.model.PresentationQueryMessage.PRESENTATION_QUERY_MESSAGE_SCOPE_TERM;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;

class PresentationQueryValidatorTest {
    public static final ObjectMapper MAPPER = JacksonJsonLd.createObjectMapper();
    private final JsonLd jsonLd = new TitaniumJsonLd(mock());
    private final JsonLdNamespace namespace = DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
    private final PresentationQueryValidator validator = new PresentationQueryValidator(namespace);

    @Test
    void validate_withScope_success() {
        var jo = createObjectBuilder()
                .add(namespace.toIri(PRESENTATION_QUERY_MESSAGE_DEFINITION_TERM), createScopeArray())
                .build();

        assertThat(validator.validate(jo)).isSucceeded();
    }

    @Test
    void validate_withEmptyScopeArray_fails() {
        var jo = createObjectBuilder()
                .add(namespace.toIri(PRESENTATION_QUERY_MESSAGE_SCOPE_TERM), createArrayBuilder().build())
                .build();

        assertThat(validator.validate(jo)).isFailed().detail().contains("Must contain either a non-null, non-empty 'scopes' property or a non-empty 'presentationDefinition' property.");
    }

    @Test
    void validate_withPresentationDefinition_success() throws JsonProcessingException {
        var presDef = PresentationDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .inputDescriptors(List.of(InputDescriptor.Builder.newInstance().id(UUID.randomUUID().toString()).constraints(new Constraints(List.of(Field.Builder.newInstance().build()))).build()))
                .build();
        var jo = createObjectBuilder()
                .add(namespace.toIri(PRESENTATION_QUERY_MESSAGE_DEFINITION_TERM), createPresentationDefArray(presDef))
                .build();

        assertThat(validator.validate(jo)).isSucceeded();
    }

    @Test
    void validate_withNone_fails() {
        var jo = createObjectBuilder().build();
        assertThat(validator.validate(jo)).isFailed().detail().contains("Must contain either a non-null, non-empty 'scopes' property or a non-empty 'presentationDefinition' property.");
    }

    @Test
    void validate_withBoth_fails() throws JsonProcessingException {
        var presDef = PresentationDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .inputDescriptors(List.of(InputDescriptor.Builder.newInstance().id(UUID.randomUUID().toString()).constraints(new Constraints(List.of(Field.Builder.newInstance().build()))).build()))
                .build();
        var jo = createObjectBuilder()
                .add(namespace.toIri(PRESENTATION_QUERY_MESSAGE_SCOPE_TERM), createScopeArray())
                .add(namespace.toIri(PRESENTATION_QUERY_MESSAGE_DEFINITION_TERM), createPresentationDefArray(presDef))
                .build();

        assertThat(validator.validate(jo)).isFailed().detail().contains("Must contain either a non-null, non-empty 'scopes' property or a non-empty 'presentationDefinition' property, not both.");
    }

    private JsonArray createScopeArray() {
        return createArrayBuilder()
                .add(createObjectBuilder().add(JsonLdKeywords.VALUE, "scope1"))
                .add(createObjectBuilder().add(JsonLdKeywords.VALUE, "scope2"))
                .build();
    }

    private JsonArray createPresentationDefArray(PresentationDefinition presDef) throws JsonProcessingException {
        var val = MAPPER.writeValueAsString(presDef);
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add(JsonLdKeywords.TYPE, JsonLdKeywords.JSON)
                        .add(JsonLdKeywords.VALUE, MAPPER.readValue(val, JsonObject.class))
                        .build())
                .build();
    }
}