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

package org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1alpha.credentialrequest.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryObject;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.model.CredentialRequestMessage.CREDENTIAL_REQUEST_MESSAGE_CREDENTIALS_TERM;

/**
 * Validator for the credential request message.
 */
public class CredentialRequestMessageValidator {

    public static Validator<JsonObject> instance(JsonLdNamespace namespace) {
        return JsonObjectValidator.newValidator()
                .verify(namespace.toIri(CREDENTIAL_REQUEST_MESSAGE_CREDENTIALS_TERM), MandatoryObject::new)
                .build();
    }
}
