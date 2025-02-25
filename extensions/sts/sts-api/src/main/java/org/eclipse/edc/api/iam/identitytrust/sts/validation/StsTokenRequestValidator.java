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

package org.eclipse.edc.api.iam.identitytrust.sts.validation;

import org.eclipse.edc.api.iam.identitytrust.sts.model.StsTokenRequest;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;

public class StsTokenRequestValidator implements Validator<StsTokenRequest> {

    public static final String GRANT_TYPE = "grant_type";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String AUDIENCE = "audience";
    private static final Map<String, Function<StsTokenRequest, ?>> FIELDS_NOT_NULL = Map.of(
            GRANT_TYPE, StsTokenRequest::getGrantType,
            CLIENT_ID, StsTokenRequest::getClientId,
            CLIENT_SECRET, StsTokenRequest::getClientSecret,
            AUDIENCE, StsTokenRequest::getAudience
    );

    @Override
    public ValidationResult validate(StsTokenRequest request) {
        var violations = new ArrayList<Violation>();
        FIELDS_NOT_NULL.forEach((fieldName, supplier) -> {
            if (supplier.apply(request) == null) {
                violations.add(Violation.violation(fieldName + " cannot be null", fieldName));
            }
        });
        if (violations.isEmpty()) {
            return ValidationResult.success();
        } else {
            return ValidationResult.failure(violations);
        }
    }
}
