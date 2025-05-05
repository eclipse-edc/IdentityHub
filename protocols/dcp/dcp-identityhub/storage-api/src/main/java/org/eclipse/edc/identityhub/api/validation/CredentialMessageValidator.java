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

package org.eclipse.edc.identityhub.api.validation;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.iam.identitytrust.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.CREDENTIALS_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.HOLDER_PID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.ISSUER_PID_TERM;
import static org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage.STATUS_TERM;
import static org.eclipse.edc.validator.spi.ValidationResult.failure;
import static org.eclipse.edc.validator.spi.ValidationResult.success;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Validates that a JsonObject representing a {@link org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialMessage} contains
 * a {@code requestId} and a {@code credentials} property
 */
public class CredentialMessageValidator implements Validator<JsonObject> {

    private final JsonLdNamespace namespace = DSPACE_DCP_NAMESPACE_V_1_0;

    @Override
    public ValidationResult validate(JsonObject input) {
        if (input == null) {
            return failure(violation("Credential message was null", "."));
        }
        var issuerPid = input.get(namespace.toIri(ISSUER_PID_TERM));
        if (isNullObject(issuerPid)) {
            return failure(violation("Must contain a 'issuerPid' property.", null));
        }

        var holderPid = input.get(namespace.toIri(HOLDER_PID_TERM));
        if (isNullObject(holderPid)) {
            return failure(violation("Must contain a 'holderPid' property.", null));
        }

        var status = input.get(namespace.toIri(STATUS_TERM));
        if (isNullObject(status)) {
            return failure(violation("Must contain a 'status' property.", null));
        }

        var credentialsObject = input.get(namespace.toIri(CREDENTIALS_TERM));
        if (isNullObject(credentialsObject)) {
            return failure(violation("Credentials array was null", null));
        }

        return success();
    }

    /**
     * Checks if the given JsonValue object is the Null-Object. Due to JSON-LD expansion, a {@code "key": null}
     * would get expanded to an array, thus a simple equals-null check is not sufficient.
     *
     * @param value the JsonValue to check
     * @return true if the JsonValue object is either null, or its value type is NULL, false otherwise
     */
    private boolean isNullObject(JsonValue value) {
        if (value instanceof JsonArray jarray) {
            if (jarray.isEmpty()) {
                return false; // empty arrays are OK
            }
            value = jarray.get(0).asJsonObject().get(JsonLdKeywords.VALUE);
            return ofNullable(value)
                    .map(jv -> jv.getValueType() == JsonValue.ValueType.NULL)
                    .orElse(false);
        }
        return value == null;
    }
}
