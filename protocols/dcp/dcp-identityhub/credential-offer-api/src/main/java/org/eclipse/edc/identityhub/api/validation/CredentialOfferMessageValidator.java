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

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.validator.spi.ValidationResult;

import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.DcpConstants.CREDENTIALS_NAMESPACE_W3C;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage.CREDENTIALS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage.CREDENTIAL_ISSUER_TERM;
import static org.eclipse.edc.validator.spi.ValidationResult.failure;
import static org.eclipse.edc.validator.spi.ValidationResult.success;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Validates that a JsonObject representing a {@link org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialOfferMessage} contains
 * a {@code issuer} identifier and a {@code credentials} property
 */
public class CredentialOfferMessageValidator extends JsonValidator {

    private final JsonLdNamespace namespace = DSPACE_DCP_NAMESPACE_V_1_0;

    private final CredentialObjectValidator credentialObjectValidator = new CredentialObjectValidator();


    @Override
    public ValidationResult validate(JsonObject input) {
        if (input == null) {
            return failure(violation("CredentialOfferMessage was null", "."));
        }
        var issuer = input.get(CREDENTIALS_NAMESPACE_W3C.toIri(CREDENTIAL_ISSUER_TERM));
        if (isNullObject(issuer)) {
            return failure(violation("Invalid format: must contain a '%s' property.".formatted(CREDENTIAL_ISSUER_TERM), null));
        }

        //sending an empty offer is nonsensical, but strictly speaking, it's allowed

        var array = input.get(namespace.toIri(CREDENTIALS_TERM));
        if (!isNullObject(array) && array.getValueType() == JsonValue.ValueType.ARRAY) {
            var results = array.asJsonArray().stream().map(jv -> {
                return credentialObjectValidator.validate(jv.asJsonObject());
            });
            return results.reduce(ValidationResult::merge).orElse(success());
        }

        return success();
    }


}
